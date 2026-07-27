package com.conclave.service;

import com.conclave.domain.*;
import com.conclave.domain.enums.ModelId;
import com.conclave.domain.enums.SenderType;
import com.conclave.exception.OrchestrationException;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.integration.adapter.*;
import com.conclave.integration.registry.ModelRegistry;
import com.conclave.repository.CanonicalMessageRepository;
import com.conclave.repository.RoleAssignmentRepository;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.WorkflowStateRepository;
import com.conclave.util.MentionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of MessageOrchestrator coordinating turns and state updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageOrchestratorImpl implements MessageOrchestrator {

    private final RoomRepository roomRepository;
    private final CanonicalMessageRepository messageRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final ModelRegistry modelRegistry;
    private final TokenUsageLogService tokenUsageLogService;

    @Lazy
    private final WorkflowStateService workflowStateService;

    @Override
    @Transactional
    public CanonicalMessage processUserTurn(UUID roomId, String userMessageContent) {
        log.info("Processing user turn for room: {}", roomId);

        // 1. Load Room
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // 2. Parse Mention
        String mention = MentionParser.extractMention(userMessageContent)
                .orElseThrow(() -> new OrchestrationException("No role mention found in message. Please mention a role using @RoleName."));

        // 3. Resolve RoleAssignment
        List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByRoomId(roomId);
        RoleAssignment matchedAssignment = roleAssignments.stream()
                .filter(ra -> ra.getRoleName().replace("-", " ").replace("_", " ").trim()
                        .equalsIgnoreCase(mention.replace("-", " ").replace("_", " ").trim()))
                .findFirst()
                .orElseThrow(() -> new OrchestrationException("No role assignment found in room " + roomId + " matching mention: @" + mention));

        String modelId = matchedAssignment.getModelId();

        // 4. Persist User Message
        CanonicalMessage userMessage = CanonicalMessage.builder()
                .room(room)
                .senderType(SenderType.USER)
                .content(userMessageContent)
                .createdAt(LocalDateTime.now())
                .isMocked(false)
                .build();
        messageRepository.save(userMessage);

        // 5. Load History & WorkflowState
        List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
        WorkflowState state = workflowStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found for room: " + roomId));

        // 6. Resolve Adapter & Client
        ProviderAdapter adapter;
        if (ModelId.GEMINI_PRO.name().equals(modelId)) {
            adapter = new GeminiAdapter();
        } else if (ModelId.FAKE_OPENAI.name().equals(modelId)) {
            adapter = new OpenAiAdapter();
        } else if (ModelId.FAKE_CLAUDE.name().equals(modelId)) {
            adapter = new ClaudeAdapter();
        } else {
            throw new OrchestrationException("Unsupported modelId: " + modelId);
        }

        ChatClient chatClient = modelRegistry.getClient(modelId);

        // 7. Translate and Invoke LLM client
        // Call adapter toProviderFormat to ensure validation and format matching
        Object requestPayload = adapter.toProviderFormat(history, state);
        log.debug("Translated context to provider format: {}", requestPayload);

        // We invoke ChatClient using a Prompt containing the user message or consolidated history
        ChatResponse chatResponse = chatClient.prompt(new Prompt(userMessageContent)).call().chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            throw new OrchestrationException("Model execution returned empty response");
        }

        String responseText = chatResponse.getResult().getOutput().getContent();

        // Wrap response text in provider-specific response object for fromProviderFormat translation
        Object responsePayload;
        if (ModelId.GEMINI_PRO.name().equals(modelId)) {
            responsePayload = GeminiAdapter.GeminiResponse.builder()
                    .candidates(List.of(new GeminiAdapter.GeminiResponse.Candidate(
                            new GeminiAdapter.Content("model", List.of(new GeminiAdapter.Part(responseText))),
                            "STOP"
                    )))
                    .build();
        } else if (ModelId.FAKE_OPENAI.name().equals(modelId)) {
            responsePayload = OpenAiAdapter.OpenAiResponse.builder()
                    .choices(List.of(new OpenAiAdapter.OpenAiResponse.Choice(
                            0,
                            new OpenAiAdapter.OpenAiMessage("assistant", responseText),
                            "stop"
                    )))
                    .build();
        } else {
            responsePayload = ClaudeAdapter.ClaudeResponse.builder()
                    .content(List.of(new ClaudeAdapter.ClaudeResponse.ContentBlock("text", responseText)))
                    .model(modelId)
                    .build();
        }

        CanonicalMessage aiResponse = adapter.fromProviderFormat(responsePayload);
        aiResponse.setRoom(room);
        aiResponse.setRoleName(matchedAssignment.getRoleName());
        aiResponse.setModelId(modelId);
        aiResponse.setIsMocked(!ModelId.GEMINI_PRO.name().equals(modelId));
        aiResponse.setCreatedAt(LocalDateTime.now());

        // Save AI response
        aiResponse = messageRepository.save(aiResponse);

        // 8. Log Tokens
        Usage usage = chatResponse.getMetadata().getUsage();
        int promptTokens = 0;
        int completionTokens = 0;
        if (usage != null) {
            promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0;
            completionTokens = usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : 0;
        }

        // If usage was empty (e.g. some client issues), fallback to heuristics calculation
        if (promptTokens == 0 && completionTokens == 0) {
            promptTokens = userMessageContent.length() / 4;
            completionTokens = responseText.length() / 4;
        }

        tokenUsageLogService.logUsage(
                roomId,
                aiResponse.getId(),
                modelId,
                promptTokens,
                completionTokens,
                aiResponse.getIsMocked()
        );

        // 9. Evaluate Context Compression
        workflowStateService.evaluateAndCompressHistory(roomId);

        return aiResponse;
    }
}
