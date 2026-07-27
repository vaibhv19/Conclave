package com.conclave.integration.client;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FakeOpenAiChatClientTest {

    private FakeOpenAiChatClient client;

    @BeforeEach
    void setUp() {
        client = new FakeOpenAiChatClient();
    }

    @Test
    void testCall_ReturnsMockResponseAndCalculatesTokens() {
        Prompt prompt = new Prompt("Analyze this database design.");
        long startTime = System.currentTimeMillis();
        ChatResponse response = client.call(prompt);
        long duration = System.currentTimeMillis() - startTime;

        // Verify latency (approx 1.5s)
        assertTrue(duration >= 1400, "Latency should be around 1.5s, but was " + duration + "ms");

        assertNotNull(response);
        assertNotNull(response.getResult());
        String text = response.getResult().getOutput().getContent();
        assertTrue(text.contains("### OpenAI Analysis Review"));
        assertTrue(text.contains("caching policies"));

        assertNotNull(response.getMetadata());
        assertNotNull(response.getMetadata().getUsage());
        assertEquals(prompt.getContents().length() / 4, response.getMetadata().getUsage().getPromptTokens());
        assertEquals(text.length() / 4, response.getMetadata().getUsage().getGenerationTokens());
        assertEquals(response.getMetadata().getUsage().getPromptTokens() + response.getMetadata().getUsage().getGenerationTokens(),
                response.getMetadata().getUsage().getTotalTokens());
    }

    @Test
    void testStream_ReturnsReactiveFlux() {
        Prompt prompt = new Prompt("Stream analyzer.");
        Flux<ChatResponse> responseFlux = client.stream(prompt);
        assertNotNull(responseFlux);

        List<ChatResponse> responses = responseFlux.collectList().block();
        assertNotNull(responses);
        assertTrue(responses.size() > 1, "Should return multiple chunks");

        StringBuilder fullText = new StringBuilder();
        for (int i = 0; i < responses.size(); i++) {
            ChatResponse res = responses.get(i);
            fullText.append(res.getResult().getOutput().getContent());

            if (i < responses.size() - 1) {
                // Usage info must be empty in intermediate chunks
                assertEquals(0L, res.getMetadata().getUsage().getPromptTokens());
                assertEquals(0L, res.getMetadata().getUsage().getGenerationTokens());
            } else {
                // Final chunk must have usage information
                assertTrue(res.getMetadata().getUsage().getPromptTokens() > 0);
                assertTrue(res.getMetadata().getUsage().getGenerationTokens() > 0);
            }
        }

        String finalResult = fullText.toString();
        assertTrue(finalResult.contains("### OpenAI Analysis Review"));
    }
}
