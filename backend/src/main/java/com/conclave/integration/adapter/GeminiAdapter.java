package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.SenderType;
import com.conclave.exception.TranslationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for translating canonical message history and workflow state to/from Gemini (Vertex AI) payload formats.
 */
public class GeminiAdapter implements ProviderAdapter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object toProviderFormat(List<CanonicalMessage> history, WorkflowState state) {
        if (history == null) {
            throw new TranslationException("History cannot be null");
        }

        List<CanonicalMessage> nonSystemHistory = new ArrayList<>();
        StringBuilder systemPrefix = new StringBuilder();

        // 1. Process system messages at the start of history
        int i = 0;
        while (i < history.size() && history.get(i).getSenderType() == SenderType.SYSTEM) {
            if (systemPrefix.length() > 0) {
                systemPrefix.append("\n");
            }
            systemPrefix.append(history.get(i).getContent());
            i++;
        }

        // 2. Verify no SYSTEM messages appear later
        while (i < history.size()) {
            CanonicalMessage msg = history.get(i);
            if (msg.getSenderType() == SenderType.SYSTEM) {
                throw new TranslationException("SYSTEM message detected in the middle of conversation history");
            }
            nonSystemHistory.add(msg);
            i++;
        }

        // 3. Extract workflow state system context
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

        // 4. Combine system messages and state context
        StringBuilder fullSystem = new StringBuilder();
        if (systemPrefix.length() > 0) {
            fullSystem.append(systemPrefix);
        }
        if (stateContext.length() > 0) {
            if (fullSystem.length() > 0) {
                fullSystem.append("\n");
            }
            fullSystem.append(stateContext);
        }
        String systemPrompt = fullSystem.toString();

        // 5. Construct Gemini turn-based content
        List<Content> contents = new ArrayList<>();
        if (nonSystemHistory.isEmpty()) {
            if (systemPrompt.isEmpty()) {
                throw new TranslationException("Cannot format empty conversation history with no state");
            }
            // If only system prompt exists, construct a single user turn
            contents.add(new Content("user", List.of(new Part(systemPrompt))));
        } else {
            String expectedRole = "user";
            for (int j = 0; j < nonSystemHistory.size(); j++) {
                CanonicalMessage msg = nonSystemHistory.get(j);
                String role;
                if (msg.getSenderType() == SenderType.USER) {
                    role = "user";
                } else if (msg.getSenderType() == SenderType.AI) {
                    role = "model";
                } else {
                    throw new TranslationException("Unsupported SenderType: " + msg.getSenderType());
                }

                if (!role.equals(expectedRole)) {
                    throw new TranslationException("Alternating role validation failed. Expected '" + expectedRole + "' but got '" + role + "' at index " + j);
                }

                String contentText = msg.getContent();
                if (j == 0 && !systemPrompt.isEmpty()) {
                    contentText = systemPrompt + "\n\n" + contentText;
                }

                contents.add(new Content(role, List.of(new Part(contentText))));
                expectedRole = "user".equals(role) ? "model" : "user";
            }
        }

        return new GeminiRequest(contents);
    }

    @Override
    public CanonicalMessage fromProviderFormat(Object response) {
        if (response == null) {
            throw new TranslationException("Response payload cannot be null");
        }

        GeminiResponse geminiResponse;
        if (response instanceof GeminiResponse) {
            geminiResponse = (GeminiResponse) response;
        } else if (response instanceof String) {
            try {
                geminiResponse = objectMapper.readValue((String) response, GeminiResponse.class);
            } catch (Exception e) {
                throw new TranslationException("Failed to parse response JSON string", e);
            }
        } else {
            try {
                geminiResponse = objectMapper.convertValue(response, GeminiResponse.class);
            } catch (Exception e) {
                throw new TranslationException("Failed to convert response payload to GeminiResponse", e);
            }
        }

        if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
            throw new TranslationException("Gemini response is missing candidates");
        }

        GeminiResponse.Candidate candidate = geminiResponse.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
            throw new TranslationException("Gemini response candidate is missing content parts");
        }

        String textContent = candidate.getContent().getParts().get(0).getText();
        if (textContent == null) {
            throw new TranslationException("Gemini response content part text is null");
        }

        return CanonicalMessage.builder()
                .senderType(SenderType.AI)
                .content(textContent)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // JSON payload structures for Gemini Vertex AI

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        private List<Content> contents;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeminiResponse {
        private List<Candidate> candidates;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Candidate {
            private Content content;
            private String finishReason;
        }
    }
}
