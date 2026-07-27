package com.conclave.integration.registry;

import com.conclave.domain.enums.ModelId;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation of the ModelRegistry mapping standard ModelIds to qualified ChatClients.
 */
@Service
public class ModelRegistryImpl implements ModelRegistry {

    private final Map<String, ChatClient> registry = new HashMap<>();
    private final Map<String, org.springframework.ai.chat.model.ChatModel> modelRegistry = new HashMap<>();

    public ModelRegistryImpl(
            @Qualifier("geminiChatClient") ChatClient geminiClient,
            @Qualifier("openAiChatClient") ChatClient openAiClient,
            @Qualifier("claudeChatClient") ChatClient claudeClient,
            @Qualifier("vertexAiGeminiChatModel") org.springframework.ai.chat.model.ChatModel geminiModel,
            com.conclave.integration.client.FakeOpenAiChatClient openAiModel,
            com.conclave.integration.client.FakeClaudeChatClient claudeModel
    ) {
        registry.put(ModelId.GEMINI_PRO.name(), geminiClient);
        registry.put(ModelId.FAKE_OPENAI.name(), openAiClient);
        registry.put(ModelId.FAKE_CLAUDE.name(), claudeClient);

        modelRegistry.put(ModelId.GEMINI_PRO.name(), geminiModel);
        modelRegistry.put(ModelId.FAKE_OPENAI.name(), openAiModel);
        modelRegistry.put(ModelId.FAKE_CLAUDE.name(), claudeModel);
    }

    @Override
    public ChatClient getClient(String modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        ChatClient client = registry.get(modelId);
        if (client == null) {
            throw new IllegalArgumentException("Unsupported or unregistered model ID: " + modelId);
        }

        return client;
    }

    @Override
    public org.springframework.ai.chat.model.ChatModel getChatModel(String modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        org.springframework.ai.chat.model.ChatModel model = modelRegistry.get(modelId);
        if (model == null) {
            throw new IllegalArgumentException("Unsupported or unregistered model ID: " + modelId);
        }

        return model;
    }
}
