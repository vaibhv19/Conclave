package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.SenderType;
import com.conclave.exception.TranslationException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LlamaAdapter implements ModelAdapter {

    @Override
    public List<Message> toModelFormat(List<CanonicalMessage> history, WorkflowState state) {
        if (history == null) {
            throw new TranslationException("History cannot be null");
        }

        List<Message> messages = new ArrayList<>();

        // 1. Build and Add System Message containing WorkflowState
        StringBuilder systemPrompt = new StringBuilder();
        if (state != null) {
            if (state.getRoom() != null && state.getRoom().getObjective() != null && !state.getRoom().getObjective().trim().isEmpty()) {
                systemPrompt.append("System Objective: ").append(state.getRoom().getObjective()).append("\n");
            }
            if (state.getCurrentDraft() != null && !state.getCurrentDraft().trim().isEmpty()) {
                systemPrompt.append("Current Draft: ").append(state.getCurrentDraft()).append("\n");
            }
            if (state.getReviewComments() != null && !state.getReviewComments().trim().isEmpty()) {
                systemPrompt.append("Review Comments: ").append(state.getReviewComments()).append("\n");
            }
        }

        if (systemPrompt.length() > 0) {
            messages.add(new SystemMessage(systemPrompt.toString().trim()));
        }

        // 2. Map history messages
        for (CanonicalMessage msg : history) {
            if (msg.getSenderType() == null) {
                throw new TranslationException("SenderType cannot be null");
            }

            switch (msg.getSenderType()) {
                case SYSTEM:
                    messages.add(new SystemMessage(msg.getContent().trim()));
                    break;
                case USER:
                    messages.add(new UserMessage(msg.getContent().trim()));
                    break;
                case AI:
                    messages.add(new AssistantMessage(msg.getContent().trim()));
                    break;
                default:
                    throw new TranslationException("Unsupported SenderType: " + msg.getSenderType());
            }
        }

        return messages;
    }

    @Override
    public CanonicalMessage fromModelFormat(String responseText) {
        if (responseText == null) {
            throw new TranslationException("Response content cannot be null");
        }
        return CanonicalMessage.builder()
                .senderType(SenderType.AI)
                .content(responseText)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
