package com.conclave.integration.registry;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Registry to dynamically resolve registered LLM ChatClient beans.
 */
public interface ModelRegistry {
    /**
     * Retrieves the ChatClient bean associated with the given model ID.
     * Throws IllegalArgumentException if the modelId is unsupported or missing.
     *
     * @param modelId The ID representing the model
     * @return The qualified ChatClient instance
     */
    ChatClient getClient(String modelId);
}
