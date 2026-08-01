package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.SenderType;
import com.conclave.exception.TranslationException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MistralAdapter implements ModelAdapter {

    @Override
    public List<Message> toModelFormat(List<CanonicalMessage> history, WorkflowState state) {
        if (history == null) {
            throw new TranslationException("History cannot be null");
        }

        List<Message> messages = new ArrayList<>();

        // 1. Build System Instruction including WorkflowState
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

        String systemStr = systemPrompt.toString().trim();
        boolean systemPrepended = false;

        for (CanonicalMessage msg : history) {
            if (msg.getSenderType() == null) {
                throw new TranslationException("SenderType cannot be null");
            }

            if (msg.getSenderType() == SenderType.USER) {
                String content = msg.getContent().trim();
                if (!systemPrepended && !systemStr.isEmpty()) {
                    content = systemStr + "\n\n" + content;
                    systemPrepended = true;
                }
                messages.add(new UserMessage(content));
            } else if (msg.getSenderType() == SenderType.AI) {
                messages.add(new AssistantMessage(msg.getContent().trim()));
            } else if (msg.getSenderType() == SenderType.SYSTEM) {
                // Since Mistral template lacks native system tag at API role level, wrap system notification in a user message
                messages.add(new UserMessage("System Notification: " + msg.getContent().trim()));
            }
        }

        // If system prompt was never prepended because history was empty or had no USER messages
        if (!systemPrepended && !systemStr.isEmpty()) {
            messages.add(0, new UserMessage(systemStr));
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
