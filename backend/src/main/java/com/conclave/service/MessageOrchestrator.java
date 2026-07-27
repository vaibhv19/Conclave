package com.conclave.service;

import com.conclave.domain.CanonicalMessage;
import java.util.UUID;

/**
 * Central coordinator responsible for processing conversation turns.
 */
public interface MessageOrchestrator {
    
    /**
     * Orchestrates a single conversation turn:
     * 1. Parses model mentions and resolves target roles.
     * 2. Persists the user message to conversation history.
     * 3. Resolves adapters and model clients from registry.
     * 4. Translates context, invokes target LLM client.
     * 5. Persists the canonical response message.
     * 6. Triggers token usage logging.
     * 7. Initiates context compression evaluation.
     *
     * @param roomId             The room ID
     * @param userMessageContent The raw user message content
     * @return The persisted generated response CanonicalMessage
     */
    CanonicalMessage processUserTurn(UUID roomId, String userMessageContent);

    /**
     * Executes a streaming turn asynchronously on Virtual Threads.
     * Handles live STOMP events, LLM client response, token usage logging,
     * and automatic pipeline pointer advancement if room status remains ACTIVE.
     *
     * @param roomId         The target room UUID
     * @param roleName       The name of the role executing the turn
     * @param promptContent  The prompt input text
     */
    void executeStreamingTurn(UUID roomId, String roleName, String promptContent);

    /**
     * Internal async helper method for executing a streaming turn transactionally.
     */
    void executeStreamingTurnAsync(UUID roomId, String roleName, String promptContent);
}
