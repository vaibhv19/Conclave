package com.conclave.config;

import com.conclave.integration.client.FakeClaudeChatClient;
import com.conclave.integration.client.FakeOpenAiChatClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Spring AI configuration class to register and qualify ChatClient beans.
 * Provides fallback implementations when live credentials are not available.
 */
@Configuration
public class SpringAiConfig {

    /**
     * Fallback ChatModel bean when live Vertex AI Gemini credentials are not configured.
     * Named 'vertexAiGeminiChatModel' to align with the standard Spring AI autoconfigured bean name.
     */
    @Bean("vertexAiGeminiChatModel")
    @ConditionalOnMissingBean(ChatModel.class)
    @Primary
    public ChatModel fallbackGeminiChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation("Gemini Fallback: [Live Gemini Credentials Not Configured] Response.")));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(call(prompt));
            }

            @Override
            public ChatOptions getDefaultOptions() {
                return null;
            }
        };
    }

    @Bean
    @Qualifier("geminiChatClient")
    public ChatClient geminiChatClient(@Qualifier("vertexAiGeminiChatModel") ChatModel geminiChatModel) {
        return ChatClient.create(geminiChatModel);
    }

    @Bean
    @Qualifier("openAiChatClient")
    public ChatClient openAiChatClient(FakeOpenAiChatClient fakeOpenAiChatModel) {
        return ChatClient.create(fakeOpenAiChatModel);
    }

    @Bean
    @Qualifier("claudeChatClient")
    public ChatClient claudeChatClient(FakeClaudeChatClient fakeClaudeChatModel) {
        return ChatClient.create(fakeClaudeChatModel);
    }

    @Bean
    public FakeOpenAiChatClient fakeOpenAiChatModel() {
        return new FakeOpenAiChatClient();
    }

    @Bean
    public FakeClaudeChatClient fakeClaudeChatModel() {
        return new FakeClaudeChatClient();
    }
}
