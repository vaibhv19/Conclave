package com.conclave.controller;

import com.conclave.domain.*;
import com.conclave.domain.enums.ModelId;
import com.conclave.domain.enums.SenderType;
import com.conclave.dto.ChatMessageRequest;
import com.conclave.dto.ws.ContentChunkEvent;
import com.conclave.dto.ws.TurnCompletedEvent;
import com.conclave.dto.ws.TurnStartedEvent;
import com.conclave.exception.OrchestrationException;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.integration.adapter.*;
import com.conclave.integration.registry.ModelRegistry;
import com.conclave.repository.CanonicalMessageRepository;
import com.conclave.repository.RoleAssignmentRepository;
import com.conclave.repository.RoomRepository;
import com.conclave.service.TokenUsageLogService;
import com.conclave.service.WorkflowStateService;
import com.conclave.util.MentionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller to handle incoming user chat messages asynchronously and stream AI agent responses.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RoomRepository roomRepository;
    private final CanonicalMessageRepository messageRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final ModelRegistry modelRegistry;
    private final TokenUsageLogService tokenUsageLogService;
    private final WorkflowStateService workflowStateService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AsyncTaskExecutor conclaveTaskExecutor;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ChatController self;

    /**
     * Endpoint to receive a user message. Validates the request, persists the user message,
     * and initiates the asynchronous AI execution turn if a mention is found, returning immediately.
     *
     * @param request The chat message request DTO
     * @return 202 Accepted HTTP response
     */
    @PostMapping("/message")
    public ResponseEntity<Void> postMessage(@RequestBody ChatMessageRequest request) {
        log.info("Received POST /api/chat/message for room: {}", request.getRoomId());

        if (request.getRoomId() == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));

        // Save User message synchronously
        CanonicalMessage userMessage = CanonicalMessage.builder()
                .room(room)
                .senderType(SenderType.USER)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .isMocked(false)
                .build();
        messageRepository.save(userMessage);

        // Check if there is an AI mention in the message
        Optional<String> mentionOpt = MentionParser.extractMention(request.getContent());
        if (mentionOpt.isPresent()) {
            String mention = mentionOpt.get();
            log.info("Parsed mention: @{}. Dispatching turn execution task asynchronously.", mention);

            // Execute the AI turn asynchronously on Virtual Threads
            conclaveTaskExecutor.execute(() -> self.processAiTurnAsync(room.getId(), request, mention));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Transactional
    public void processAiTurnAsync(UUID roomId, ChatMessageRequest request, String mention) {
        try {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
            // 1. Resolve Role Assignment
            List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByRoomId(roomId);
            RoleAssignment matchedAssignment = roleAssignments.stream()
                    .filter(ra -> ra.getRoleName().replace("-", " ").replace("_", " ").trim()
                            .equalsIgnoreCase(mention.replace("-", " ").replace("_", " ").trim()))
                    .findFirst()
                    .orElseThrow(() -> new OrchestrationException("No role assignment found matching mention: @" + mention));

            String modelId = matchedAssignment.getModelId();
            boolean isMocked = !ModelId.GEMINI_PRO.name().equals(modelId);

            // 2. Broadcast TURN_STARTED event
            TurnStartedEvent startedEvent = new TurnStartedEvent(matchedAssignment.getRoleName(), modelId, isMocked);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, startedEvent);

            // 3. Resolve Client & Adapter
            ChatClient chatClient = modelRegistry.getClient(modelId);
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

            // Load full history & WorkflowState for adapter translation validation
            List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
            WorkflowState state = workflowStateService.getWorkflowState(roomId);
            Object requestPayload = adapter.toProviderFormat(history, state);
            log.debug("Validated adapter request translation: {}", requestPayload);

            CanonicalMessage aiMessage = CanonicalMessage.builder()
                    .room(room)
                    .senderType(SenderType.AI)
                    .roleName(matchedAssignment.getRoleName())
                    .modelId(modelId)
                    .isMocked(isMocked)
                    .content("")
                    .createdAt(LocalDateTime.now())
                    .build();
            aiMessage = messageRepository.save(aiMessage);
            UUID aiMessageId = aiMessage.getId();

            // Stream response from model
            ChatModel chatModel = modelRegistry.getChatModel(modelId);
            Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt(request.getContent()));

            // Consume stream blocking-style since we are executing on a Virtual Thread
            Iterable<ChatResponse> chunks = responseFlux.toIterable();
            StringBuilder fullContentBuilder = new StringBuilder();

            int promptTokens = 0;
            int completionTokens = 0;

            for (ChatResponse chunk : chunks) {
                if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
                    String chunkText = chunk.getResult().getOutput().getContent();
                    if (chunkText != null) {
                        fullContentBuilder.append(chunkText);

                        // Broadcast chunk to clients
                        ContentChunkEvent chunkEvent = new ContentChunkEvent(chunkText, aiMessageId);
                        messagingTemplate.convertAndSend("/topic/room/" + roomId, chunkEvent);
                    }
                }

                // Extract token usage metadata from chunks if present
                if (chunk.getMetadata() != null && chunk.getMetadata().getUsage() != null) {
                    Usage usage = chunk.getMetadata().getUsage();
                    if (usage.getPromptTokens() != null) {
                        promptTokens = usage.getPromptTokens().intValue();
                    }
                    if (usage.getGenerationTokens() != null) {
                        completionTokens = usage.getGenerationTokens().intValue();
                    }
                }
            }

            String fullContent = fullContentBuilder.toString();
            log.info("AI response streaming complete. Reconstructed length: {}", fullContent.length());

            // 5. Wrap response payload and convert back via adapter to validate format
            Object responsePayload;
            if (ModelId.GEMINI_PRO.name().equals(modelId)) {
                responsePayload = GeminiAdapter.GeminiResponse.builder()
                        .candidates(List.of(new GeminiAdapter.GeminiResponse.Candidate(
                                new GeminiAdapter.Content("model", List.of(new GeminiAdapter.Part(fullContent))),
                                "STOP"
                        )))
                        .build();
            } else if (ModelId.FAKE_OPENAI.name().equals(modelId)) {
                responsePayload = OpenAiAdapter.OpenAiResponse.builder()
                        .choices(List.of(new OpenAiAdapter.OpenAiResponse.Choice(
                                0,
                                new OpenAiAdapter.OpenAiMessage("assistant", fullContent),
                                "stop"
                        )))
                        .build();
            } else {
                responsePayload = ClaudeAdapter.ClaudeResponse.builder()
                        .content(List.of(new ClaudeAdapter.ClaudeResponse.ContentBlock("text", fullContent)))
                        .model(modelId)
                        .build();
            }

            CanonicalMessage validatedMessage = adapter.fromProviderFormat(responsePayload);
            log.debug("Validated parsed message content: {}", validatedMessage.getContent());

            // Update the placeholder AI message with full content
            aiMessage.setContent(fullContent);
            messageRepository.save(aiMessage);

            // 6. Token Usage Persistence
            if (promptTokens == 0 && completionTokens == 0) {
                promptTokens = request.getContent().length() / 4;
                completionTokens = fullContent.length() / 4;
            }

            tokenUsageLogService.logUsage(
                    roomId,
                    aiMessageId,
                    modelId,
                    promptTokens,
                    completionTokens,
                    isMocked
            );

            // 7. Context Janitor evaluation
            workflowStateService.evaluateAndCompressHistory(roomId);

            // 8. Broadcast TURN_COMPLETED event
            TurnCompletedEvent completedEvent = new TurnCompletedEvent(
                    aiMessageId,
                    fullContent,
                    promptTokens,
                    completionTokens
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId, completedEvent);

        } catch (Exception e) {
            log.error("Exception occurred during asynchronous AI turn execution for room {}", roomId, e);
            // Optionally notify clients of error
            messagingTemplate.convertAndSend("/topic/room/" + roomId,
                    new com.conclave.dto.ws.SystemInterventionEvent("Execution error: " + e.getMessage()));
        }
    }
}
