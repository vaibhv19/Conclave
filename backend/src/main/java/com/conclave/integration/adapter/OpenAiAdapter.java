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
 * Adapter for translating canonical message history and workflow state to/from OpenAI payload formats.
 */
public class OpenAiAdapter implements ProviderAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state) {
        if (history == null) {
            throw new TranslationException("History cannot be null");
        }

        List<OpenAiMessage> messages = new ArrayList<>();

        // 1. Prepend workflow state as a system instruction message
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
            messages.add(new OpenAiMessage("system", stateContext.toString().trim()));
        }

        // 2. Map canonical history to flat list of messages
        String resolvedModel = "gpt-4";
        for (CanonicalMessage msg : history) {
            if (msg.getSenderType() == null) {
                throw new TranslationException("SenderType cannot be null in history messages");
            }

            String role;
            switch (msg.getSenderType()) {
                case SYSTEM:
                    role = "system";
                    break;
                case USER:
                    role = "user";
                    break;
                case AI:
                    role = "assistant";
                    break;
                default:
                    throw new TranslationException("Unsupported SenderType: " + msg.getSenderType());
            }

            messages.add(new OpenAiMessage(role, msg.getContent()));
            
            if (msg.getModelId() != null && !msg.getModelId().trim().isEmpty()) {
                resolvedModel = msg.getModelId();
            }
        }

        if (messages.isEmpty()) {
            throw new TranslationException("Cannot format empty conversation history with no state");
        }

        return new OpenAiRequest(resolvedModel, messages);
    }

    @Override
    public CanonicalMessage fromProviderFormat(Object response) {
        if (response == null) {
            throw new TranslationException("Response payload cannot be null");
        }

        OpenAiResponse openAiResponse;
        if (response instanceof OpenAiResponse) {
            openAiResponse = (OpenAiResponse) response;
        } else if (response instanceof String) {
            try {
                openAiResponse = objectMapper.readValue((String) response, OpenAiResponse.class);
            } catch (Exception e) {
                throw new TranslationException("Failed to parse response JSON string", e);
            }
        } else {
            try {
                openAiResponse = objectMapper.convertValue(response, OpenAiResponse.class);
            } catch (Exception e) {
                throw new TranslationException("Failed to convert response payload to OpenAiResponse", e);
            }
        }

        if (openAiResponse.getChoices() == null || openAiResponse.getChoices().isEmpty()) {
            throw new TranslationException("OpenAI response is missing choices");
        }

        OpenAiResponse.Choice choice = openAiResponse.getChoices().get(0);
        if (choice.getMessage() == null) {
            throw new TranslationException("OpenAI response choice is missing message");
        }

        String textContent = choice.getMessage().getContent();
        if (textContent == null) {
            throw new TranslationException("OpenAI response message content is null");
        }

        return CanonicalMessage.builder()
                .senderType(SenderType.AI)
                .content(textContent)
                .modelId(openAiResponse.getModel())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // JSON payload structures for OpenAI Chat Completion API

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiRequest {
        private String model;
        private List<OpenAiMessage> messages;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiMessage {
        private String role;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OpenAiResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            private int index;
            private OpenAiMessage message;
            
            @JsonProperty("finish_reason")
            private String finishReason;
        }
    }
}
