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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.core.task.AsyncTaskExecutor;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.dto.ws.*;
import reactor.core.publisher.Flux;

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
    private final SimpMessagingTemplate messagingTemplate;
    private final AsyncTaskExecutor conclaveTaskExecutor;

    @Lazy
    private final WorkflowStateService workflowStateService;

    @org.springframework.beans.factory.annotation.Autowired
    @Lazy
    private MessageOrchestrator self;

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
        ModelAdapter adapter = modelRegistry.getAdapter(modelId);
        ChatClient chatClient = modelRegistry.getClient(modelId);

        // 7. Translate and Invoke LLM client
        // Call adapter toModelFormat to ensure validation and format matching
        List<org.springframework.ai.chat.messages.Message> messages = adapter.toModelFormat(history, state);
        log.debug("Translated context to model format: {}", messages);

        // We invoke ChatClient using a Prompt containing the consolidated history
        ChatResponse chatResponse = chatClient.prompt(new Prompt(messages)).call().chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            throw new OrchestrationException("Model execution returned empty response");
        }

        String responseText = chatResponse.getResult().getOutput().getContent();

        CanonicalMessage aiResponse = adapter.fromModelFormat(responseText);
        aiResponse.setRoom(room);
        aiResponse.setRoleName(matchedAssignment.getRoleName());
        aiResponse.setModelId(modelId);
        aiResponse.setIsMocked(false); // All inference calls are real
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
                false
        );

        // 9. Evaluate Context Compression
        workflowStateService.evaluateAndCompressHistory(roomId);

        return aiResponse;
    }

    @Override
    public void executeStreamingTurn(UUID roomId, String roleName, String promptContent) {
        log.info("Scheduling streaming turn asynchronously for room: {}, role: {}", roomId, roleName);
        conclaveTaskExecutor.execute(() -> self.executeStreamingTurnAsync(roomId, roleName, promptContent));
    }

    @Override
    @Transactional
    public void executeStreamingTurnAsync(UUID roomId, String roleName, String promptContent) {
        log.info("Starting async streaming turn for room: {}, role: {}", roomId, roleName);
        try {
            // Lock room to prevent concurrent modifications during turn execution
            Room room = roomRepository.findWithLockById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

            // Validate that Room is ACTIVE before executing step
            if (room.getStatus() != RoomStatus.ACTIVE) {
                log.info("Halt turn execution: Room {} status is {}", roomId, room.getStatus());
                return;
            }

            // 1. Resolve Role Assignment
            List<RoleAssignment> roleAssignments = roleAssignmentRepository.findByRoomId(roomId);
            RoleAssignment matchedAssignment = roleAssignments.stream()
                    .filter(ra -> ra.getRoleName().replace("-", " ").replace("_", " ").trim()
                            .equalsIgnoreCase(roleName.replace("-", " ").replace("_", " ").trim()))
                    .findFirst()
                    .orElseThrow(() -> new OrchestrationException("No role assignment found matching role: " + roleName));

            String modelId = matchedAssignment.getModelId();

            // 2. Broadcast TURN_STARTED event
            TurnStartedEvent startedEvent = new TurnStartedEvent(matchedAssignment.getRoleName(), modelId, false);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, startedEvent);

            // 3. Resolve Adapter
            ModelAdapter adapter = modelRegistry.getAdapter(modelId);

            // Load full history & WorkflowState for adapter translation validation
            List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
            WorkflowState state = workflowStateService.getWorkflowState(roomId);
            List<org.springframework.ai.chat.messages.Message> messages = adapter.toModelFormat(history, state);
            log.debug("Validated adapter request translation: {}", messages);

            // 4. Save placeholder AI message to obtain generated ID
            CanonicalMessage aiMessage = CanonicalMessage.builder()
                    .room(room)
                    .senderType(SenderType.AI)
                    .roleName(matchedAssignment.getRoleName())
                    .modelId(modelId)
                    .isMocked(false)
                    .content("")
                    .createdAt(LocalDateTime.now())
                    .build();
            aiMessage = messageRepository.save(aiMessage);
            UUID aiMessageId = aiMessage.getId();

            // Stream response from model
            ChatModel chatModel = modelRegistry.getChatModel(modelId);
            Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt(messages));

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

            // 5. Convert back via adapter to validate format
            CanonicalMessage validatedMessage = adapter.fromModelFormat(fullContent);
            log.debug("Validated parsed message content: {}", validatedMessage.getContent());

            // Update the placeholder AI message with full content
            aiMessage.setContent(fullContent);
            messageRepository.save(aiMessage);

            // 6. Token Usage Persistence
            if (promptTokens == 0 && completionTokens == 0) {
                promptTokens = promptContent.length() / 4;
                completionTokens = fullContent.length() / 4;
            }

            tokenUsageLogService.logUsage(
                    roomId,
                    aiMessageId,
                    modelId,
                    promptTokens,
                    completionTokens,
                    false
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

            // 9. Pipeline Sequential Auto-Advance Execution
            // Re-acquire lock to check if state changed during streaming execution (e.g. user paused)
            Room updatedRoom = roomRepository.findWithLockById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

            if (updatedRoom.getStatus() == RoomStatus.ACTIVE) {
                List<String> sequence = updatedRoom.getPipelineSequenceList();
                if (!sequence.isEmpty() && updatedRoom.getCurrentPipelineIndex() != null) {
                    int nextIndex = updatedRoom.getCurrentPipelineIndex() + 1;
                    if (nextIndex < sequence.size()) {
                        updatedRoom.setCurrentPipelineIndex(nextIndex);
                        roomRepository.save(updatedRoom);

                        String nextRole = sequence.get(nextIndex);
                        log.info("Auto-advancing pipeline to step index {} (role: {}). Triggering next turn.", nextIndex, nextRole);

                        // Trigger next step recursively using self to ensure proper transaction scoping
                        self.executeStreamingTurn(roomId, nextRole, promptContent);
                    } else {
                        log.info("Pipeline sequence completed (reached end index {}).", nextIndex - 1);
                    }
                }
            } else {
                log.info("Halt sequential pipeline advance: Room status is currently {}", updatedRoom.getStatus());
            }

        } catch (Exception e) {
            log.error("Exception occurred during asynchronous AI turn execution for room {}", roomId, e);
            messagingTemplate.convertAndSend("/topic/room/" + roomId,
                    new com.conclave.dto.ws.SystemInterventionEvent("Execution error: " + e.getMessage()));
        }
    }
}
