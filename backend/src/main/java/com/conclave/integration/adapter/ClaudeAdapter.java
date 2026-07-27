package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.SenderType;
import com.conclave.exception.TranslationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for translating canonical message history and workflow state to/from Claude (Anthropic) payload formats.
 */
public class ClaudeAdapter implements ProviderAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state) {
        if (history == null) {
            throw new TranslationException("History cannot be null");
        }

        List<ClaudeMessage> messages = new ArrayList<>();
        StringBuilder systemBuilder = new StringBuilder();

        // 1. Gather all SYSTEM messages in the history (wherever they are)
        // Wait, normally system messages are at the beginning, but Claude specifies extracting them out.
        // Let's filter them out completely and concatenate them.
        for (CanonicalMessage msg : history) {
            if (msg.getSenderType() == SenderType.SYSTEM) {
                if (systemBuilder.length() > 0) {
                    systemBuilder.append("\n");
                }
                systemBuilder.append(msg.getContent());
            }
        }

        // 2. Append workflow state context to the system prompt
        StringBuilder stateContext = new StringBuilder();
        if (state != null) {
            if (state.getRoom() != null && state.getRoom().getObjective() != null && !state.getRoom().getObjective().trim().isEmpty()) {
                stateContext.append("System Objective: ").append(state.getRoom().getObjective()).append("\n");
            }
            if (state.getCurrentDraft() != null && !state.getCurrentDraft().trim().isEmpty()) {
                stateContext.append("Current Draft: ").append(state.getCurrentDraft()).append("\n");
            }
            if (state.getReviewComments() != null && !state.getReviewComments().trim().isEmpty()) {
                stateContext.append("Review Comments: ").append(state.getReviewComments()).append("\n");
            }
        }

        if (stateContext.length() > 0) {
            if (systemBuilder.length() > 0) {
                systemBuilder.append("\n");
            }
            systemBuilder.append(stateContext);
        }
        String systemPrompt = systemBuilder.length() > 0 ? systemBuilder.toString() : null;

        // 3. Map non-SYSTEM messages and validate alternating roles
        String resolvedModel = "claude-3-opus-20240229";
        String expectedRole = "user";
        for (CanonicalMessage msg : history) {
            if (msg.getSenderType() == SenderType.SYSTEM) {
                continue; // System messages are extracted already
            }

            String role;
            if (msg.getSenderType() == SenderType.USER) {
                role = "user";
            } else if (msg.getSenderType() == SenderType.AI) {
                role = "assistant";
            } else {
                throw new TranslationException("Unsupported SenderType: " + msg.getSenderType());
            }

            if (!role.equals(expectedRole)) {
                throw new TranslationException("Alternating role validation failed. Expected '" + expectedRole + "' but got '" + role + "'");
            }

            messages.add(new ClaudeMessage(role, msg.getContent()));
            expectedRole = "user".equals(role) ? "assistant" : "user";

            if (msg.getModelId() != null && !msg.getModelId().trim().isEmpty()) {
                resolvedModel = msg.getModelId();
            }
        }

        if (messages.isEmpty()) {
            throw new TranslationException("Claude messages array cannot be empty");
        }

        return new ClaudeRequest(resolvedModel, systemPrompt, messages, 1024);
    }

    @Override
    public CanonicalMessage fromProviderFormat(Object response) {
        if (response == null) {
            throw new TranslationException("Response payload cannot be null");
        }

        ClaudeResponse claudeResponse;
        if (response instanceof ClaudeResponse) {
            claudeResponse = (ClaudeResponse) response;
        } else if (response instanceof String) {
            try {
                claudeResponse = objectMapper.readValue((String) response, ClaudeResponse.class);
            } catch (Exception e) {
                throw new TranslationException("Failed to parse response JSON string", e);
            }
        } else {
            try {
                claudeResponse = objectMapper.convertValue(response, ClaudeResponse.class);
            } catch (Exception e) {
                throw new TranslationException("Failed to convert response payload to ClaudeResponse", e);
            }
        }

        if (claudeResponse.getContent() == null || claudeResponse.getContent().isEmpty()) {
            throw new TranslationException("Claude response is missing content blocks");
        }

        ClaudeResponse.ContentBlock contentBlock = claudeResponse.getContent().get(0);
        if (!"text".equals(contentBlock.getType())) {
            throw new TranslationException("Claude response content block type is not text");
        }

        String textContent = contentBlock.getText();
        if (textContent == null) {
            throw new TranslationException("Claude response text content is null");
        }

        return CanonicalMessage.builder()
                .senderType(SenderType.AI)
                .content(textContent)
                .modelId(claudeResponse.getModel())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // JSON payload structures for Claude API

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaudeRequest {
        private String model;
        private String system;
        private List<ClaudeMessage> messages;
        
        @JsonProperty("max_tokens")
        private Integer maxTokens;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaudeMessage {
        private String role;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaudeResponse {
        private String id;
        private String type;
        private String role;
        private List<ContentBlock> content;
        private String model;
        
        @JsonProperty("stop_reason")
        private String stopReason;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ContentBlock {
            private String type;
            private String text;
        }
    }
}
