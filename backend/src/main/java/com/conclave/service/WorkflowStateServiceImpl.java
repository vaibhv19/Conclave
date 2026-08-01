package com.conclave.service;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.ModelId;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.integration.registry.ModelRegistry;
import com.conclave.repository.CanonicalMessageRepository;
import com.conclave.repository.WorkflowStateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of WorkflowStateService handling document updates and Janitor context compression.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowStateServiceImpl implements WorkflowStateService {

    private final WorkflowStateRepository workflowStateRepository;
    private final CanonicalMessageRepository messageRepository;
    private final ModelRegistry modelRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public WorkflowState getWorkflowState(UUID roomId) {
        return workflowStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found for room: " + roomId));
    }

    @Override
    @Transactional
    public void evaluateAndCompressHistory(UUID roomId) {
        List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
        log.info("Checking history size for room: {}. Message count: {}", roomId, history.size());

        if (history.size() <= 10) {
            return;
        }

        log.info("Triggering context compression Janitor turn for room: {}", roomId);

        WorkflowState state = getWorkflowState(roomId);

        // 1. Format history messages for context summarization
        StringBuilder historySb = new StringBuilder();
        for (CanonicalMessage message : history) {
            historySb.append(String.format("[%s - %s]: %s\n",
                    message.getSenderType(),
                    message.getRoleName() != null ? message.getRoleName() : "User",
                    message.getContent()));
        }
        String formattedHistory = historySb.toString();

        // 2. Build the summarizer system prompt
        String summarizerPrompt = String.format(
                "You are Conclave Janitor, a context compression assistant.\n" +
                "Your task is to review the active conversation history and update the current draft and review comments.\n\n" +
                "Current Draft:\n%s\n\n" +
                "Review Comments:\n%s\n\n" +
                "New Message History:\n%s\n\n" +
                "Instructions:\n" +
                "1. Incorporate any agreed-upon changes from the conversation history into the document draft.\n" +
                "2. Extract any unresolved critique points or feedback as review comments.\n" +
                "3. Output the result strictly in JSON format with exactly two keys: 'currentDraft' and 'reviewComments'. " +
                "Do not include any other conversational filler text.",
                state.getCurrentDraft(),
                state.getReviewComments(),
                formattedHistory
        );

        // 3. Invoke Llama 3 summarizer client
        ChatClient llamaClient = modelRegistry.getClient(ModelId.LLAMA3.name());
        String responseText = llamaClient.prompt().user(summarizerPrompt).call().content();

        // 4. Update state from response payload
        updateStateFromResponse(state, responseText);
        state.setLastUpdatedAt(LocalDateTime.now());
        workflowStateRepository.save(state);

        // 5. Purge middle messages, retaining the first message (context foundation) and the last 2 messages (short-term memory)
        List<CanonicalMessage> toDelete = new ArrayList<>();
        for (int i = 1; i < history.size() - 2; i++) {
            toDelete.add(history.get(i));
        }

        log.info("Purging {} middle messages from history.", toDelete.size());
        messageRepository.deleteAll(toDelete);
    }

    private void updateStateFromResponse(WorkflowState state, String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            return;
        }

        try {
            // Strip markdown block markers if present
            String cleanJson = responseText.trim();
            if (cleanJson.contains("```json")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```json") + 7);
                if (cleanJson.contains("```")) {
                    cleanJson = cleanJson.substring(0, cleanJson.indexOf("```"));
                }
            } else if (cleanJson.contains("```")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```") + 3);
                if (cleanJson.contains("```")) {
                    cleanJson = cleanJson.substring(0, cleanJson.indexOf("```"));
                }
            }
            cleanJson = cleanJson.trim();

            Map<String, String> map = objectMapper.readValue(cleanJson, new TypeReference<Map<String, String>>() {});

            if (map.containsKey("currentDraft")) {
                state.setCurrentDraft(map.get("currentDraft"));
            }
            if (map.containsKey("reviewComments")) {
                state.setReviewComments(map.get("reviewComments"));
            }
        } catch (Exception e) {
            log.error("Failed to parse Llama 3 Janitor JSON response. Falling back to setting full response as draft.", e);
            state.setCurrentDraft(responseText);
            state.setReviewComments("JSON parsing failed. Review comments unresolved.");
        }
    }
}
