package com.conclave.integration.registry;

import com.conclave.domain.enums.ModelId;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class ModelRegistryTest {

    @Autowired
    private ModelRegistry modelRegistry;

    @Test
    void testRegistryResolvesAllSupportedModels() {
        assertNotNull(modelRegistry);

        // 1. Verify GEMINI_PRO maps to fallback/live client
        ChatClient geminiClient = modelRegistry.getClient(ModelId.GEMINI_PRO.name());
        assertNotNull(geminiClient);
        String geminiResponse = geminiClient.prompt().user("Hello").call().content();
        assertNotNull(geminiResponse);
        // Under dev/test profiles, this will fall back to our dummy model
        assertTrue(geminiResponse.contains("Gemini Fallback") || geminiResponse.length() > 0);

        // 2. Verify FAKE_OPENAI maps to FakeOpenAiChatClient
        ChatClient openAiClient = modelRegistry.getClient(ModelId.FAKE_OPENAI.name());
        assertNotNull(openAiClient);
        String openAiResponse = openAiClient.prompt().user("Hello").call().content();
        assertNotNull(openAiResponse);
        assertTrue(openAiResponse.contains("### OpenAI Analysis Review"));

        // 3. Verify FAKE_CLAUDE maps to FakeClaudeChatClient
        ChatClient claudeClient = modelRegistry.getClient(ModelId.FAKE_CLAUDE.name());
        assertNotNull(claudeClient);
        String claudeResponse = claudeClient.prompt().user("Hello").call().content();
        assertNotNull(claudeResponse);
        assertTrue(claudeResponse.contains("### Claude Code Critique"));
    }

    @Test
    void testRegistryThrowsOnUnsupportedModel() {
        assertThrows(IllegalArgumentException.class, () -> modelRegistry.getClient("UNSUPPORTED_LLM_MODEL"));
    }

    @Test
    void testRegistryThrowsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> modelRegistry.getClient(null));
    }
}
