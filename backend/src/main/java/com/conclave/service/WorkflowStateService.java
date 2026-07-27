package com.conclave.service;

import com.conclave.domain.WorkflowState;
import java.util.UUID;

/**
 * Handles WorkflowState management and conversation history context compression.
 */
public interface WorkflowStateService {

    /**
     * Checks the history length for a room and triggers context compression (Janitor turn)
     * if the history size exceeds 10 messages.
     *
     * @param roomId The room ID to evaluate
     */
    void evaluateAndCompressHistory(UUID roomId);

    /**
     * Retrieves the current WorkflowState for a given room.
     *
     * @param roomId The room ID
     * @return The active WorkflowState
     */
    WorkflowState getWorkflowState(UUID roomId);
}
