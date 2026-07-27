package com.conclave.integration.client;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.PromptMetadata;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fake OpenAI ChatClient that implements the Spring AI ChatModel contract.
 * Simulates latency, chunked streaming, and token estimation.
 */
public class FakeOpenAiChatClient implements ChatModel {

    private static final String MOCK_RESPONSE =
            "### OpenAI Analysis Review\n" +
            "The proposed structure is highly optimized. I suggest the following refinements:\n" +
            "1. Optimize caching policies to reduce database query load.\n" +
            "2. Implement retry logic on external API integrations.";

    @Override
    public ChatResponse call(Prompt prompt) {
        // Simulate network latency (1.5 seconds)
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long promptTokens = estimatePromptTokens(prompt);
        long generationTokens = estimateTokens(MOCK_RESPONSE);

        return createChatResponse(MOCK_RESPONSE, promptTokens, generationTokens);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        long promptTokens = estimatePromptTokens(prompt);
        long generationTokens = estimateTokens(MOCK_RESPONSE);

        // Split by whitespace to simulate word-by-word streaming
        String[] words = MOCK_RESPONSE.split(" ");
        List<ChatResponse> responses = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            // Append space to all except last word
            String chunkText = words[i] + (i < words.length - 1 ? " " : "");
            
            // Usage info is only present in the final chunk
            long pTokens = (i == words.length - 1) ? promptTokens : 0L;
            long gTokens = (i == words.length - 1) ? generationTokens : 0L;

            responses.add(createChatResponse(chunkText, pTokens, gTokens));
        }

        // Stream responses with a 50ms delay per element
        return Flux.fromIterable(responses)
                .delayElements(Duration.ofMillis(50));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }

    private long estimatePromptTokens(Prompt prompt) {
        if (prompt == null || prompt.getContents() == null) {
            return 0L;
        }
        return prompt.getContents().length() / 4;
    }

    private long estimateTokens(String text) {
        if (text == null) {
            return 0L;
        }
        return text.length() / 4;
    }

    private ChatResponse createChatResponse(String text, long promptTokens, long generationTokens) {
        Generation generation = new Generation(text);
        
        Usage usage = new Usage() {
            @Override
            public Long getPromptTokens() {
                return promptTokens;
            }

            @Override
            public Long getGenerationTokens() {
                return generationTokens;
            }

            @Override
            public Long getTotalTokens() {
                return promptTokens + generationTokens;
            }
        };

        ChatResponseMetadata metadata = new SimpleChatResponseMetadata(usage);

        return new ChatResponse(List.of(generation), metadata);
    }

    private static class SimpleChatResponseMetadata extends java.util.HashMap<String, Object> implements ChatResponseMetadata {
        private final Usage usage;

        public SimpleChatResponseMetadata(Usage usage) {
            this.usage = usage;
        }

        @Override
        public RateLimit getRateLimit() {
            return null;
        }

        @Override
        public PromptMetadata getPromptMetadata() {
            return null;
        }

        @Override
        public Usage getUsage() {
            return usage;
        }
    }
}
