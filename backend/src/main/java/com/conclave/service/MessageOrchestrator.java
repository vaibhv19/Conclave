package com.conclave.service;

import java.util.UUID;

/**
 * Central coordinator responsible for processing conversation turns.
 */
public interface MessageOrchestrator {

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
