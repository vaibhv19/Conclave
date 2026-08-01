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

        // 1. Verify LLAMA3
        ChatClient llamaClient = modelRegistry.getClient(ModelId.LLAMA3.name());
        assertNotNull(llamaClient);
        assertNotNull(modelRegistry.getChatModel(ModelId.LLAMA3.name()));
        assertNotNull(modelRegistry.getAdapter(ModelId.LLAMA3.name()));

        // 2. Verify MISTRAL
        ChatClient mistralClient = modelRegistry.getClient(ModelId.MISTRAL.name());
        assertNotNull(mistralClient);
        assertNotNull(modelRegistry.getChatModel(ModelId.MISTRAL.name()));
        assertNotNull(modelRegistry.getAdapter(ModelId.MISTRAL.name()));

        // 3. Verify GEMMA
        ChatClient gemmaClient = modelRegistry.getClient(ModelId.GEMMA.name());
        assertNotNull(gemmaClient);
        assertNotNull(modelRegistry.getChatModel(ModelId.GEMMA.name()));
        assertNotNull(modelRegistry.getAdapter(ModelId.GEMMA.name()));
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
