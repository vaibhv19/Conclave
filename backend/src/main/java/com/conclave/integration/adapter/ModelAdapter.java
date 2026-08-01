package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.WorkflowState;
import org.springframework.ai.chat.messages.Message;
import java.util.List;

public interface ModelAdapter {
    /**
     * Translates the canonical conversation history combined with the WorkflowState summary 
     * into a list of Spring AI Messages for model consumption.
     *
     * @param history The canonical conversation history
     * @param state   The current workflow state summary
     * @return The list of Spring AI Message objects
     */
    List<Message> toModelFormat(List<CanonicalMessage> history, WorkflowState state);

    /**
     * Translates a response string back into a CanonicalMessage.
     *
     * @param responseText The response text returned by the model
     * @return The translated CanonicalMessage
     */
    CanonicalMessage fromModelFormat(String responseText);
}
