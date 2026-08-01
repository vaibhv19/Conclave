package com.conclave.integration.registry;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import reactor.core.publisher.Flux;

/**
 * A delegating ChatModel wrapper that overrides the prompt options to specify the desired model ID at runtime.
 */
public class OllamaChatModelWrapper implements ChatModel {

    private final ChatModel delegate;
    private final String modelName;

    public OllamaChatModelWrapper(ChatModel delegate, String modelName) {
        this.delegate = delegate;
        this.modelName = modelName;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return delegate.call(overrideModel(prompt));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(overrideModel(prompt));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        OllamaOptions options = new OllamaOptions();
        options.setModel(modelName);
        return options;
    }

    private Prompt overrideModel(Prompt prompt) {
        OllamaOptions customOptions = new OllamaOptions();
        customOptions.setModel(modelName);

        if (prompt.getOptions() != null) {
            // If the prompt has existing OllamaOptions, we merge them
            if (prompt.getOptions() instanceof OllamaOptions) {
                OllamaOptions existing = (OllamaOptions) prompt.getOptions();
                // Merge other options if necessary, but at least set the model
                existing.setModel(modelName);
                return new Prompt(prompt.getInstructions(), existing);
            }
        }

        return new Prompt(prompt.getInstructions(), customOptions);
    }
}
