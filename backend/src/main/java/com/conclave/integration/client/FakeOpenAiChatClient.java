package com.conclave.integration.client;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public class FakeOpenAiChatClient implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        return null;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return null;
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }
}
