package com.conclave.integration.registry;

import com.conclave.domain.enums.ModelId;
import com.conclave.integration.adapter.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation of the ModelRegistry mapping standard ModelIds to qualified ChatClients, ChatModels, and ModelAdapters.
 */
@Service
public class ModelRegistryImpl implements ModelRegistry {

    private final Map<String, ChatClient> registry = new HashMap<>();
    private final Map<String, ChatModel> modelRegistry = new HashMap<>();
    private final Map<String, ModelAdapter> adapterRegistry = new HashMap<>();

    public ModelRegistryImpl(OllamaChatModel ollamaChatModel) {
        // Register Local Models
        ChatModel llama3Model = new OllamaChatModelWrapper(ollamaChatModel, "llama3");
        modelRegistry.put(ModelId.LLAMA3.name(), llama3Model);
        registry.put(ModelId.LLAMA3.name(), ChatClient.create(llama3Model));
        adapterRegistry.put(ModelId.LLAMA3.name(), new LlamaAdapter());

        ChatModel mistralModel = new OllamaChatModelWrapper(ollamaChatModel, "mistral");
        modelRegistry.put(ModelId.MISTRAL.name(), mistralModel);
        registry.put(ModelId.MISTRAL.name(), ChatClient.create(mistralModel));
        adapterRegistry.put(ModelId.MISTRAL.name(), new MistralAdapter());

        ChatModel gemmaModel = new OllamaChatModelWrapper(ollamaChatModel, "gemma");
        modelRegistry.put(ModelId.GEMMA.name(), gemmaModel);
        registry.put(ModelId.GEMMA.name(), ChatClient.create(gemmaModel));
        adapterRegistry.put(ModelId.GEMMA.name(), new GemmaAdapter());
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
    public ChatModel getChatModel(String modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        ChatModel model = modelRegistry.get(modelId);
        if (model == null) {
            throw new IllegalArgumentException("Unsupported or unregistered model ID: " + modelId);
        }

        return model;
    }

    @Override
    public ModelAdapter getAdapter(String modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        ModelAdapter adapter = adapterRegistry.get(modelId);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported or unregistered model ID: " + modelId);
        }

        return adapter;
    }
}
