package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.Room;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.SenderType;
import com.conclave.exception.TranslationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiAdapterTest {

    private OpenAiAdapter adapter;
    private Room room;
    private WorkflowState workflowState;

    @BeforeEach
    void setUp() {
        adapter = new OpenAiAdapter();
        room = Room.builder()
                .name("OpenAI Room")
                .objective("Generate a marketing slogan.")
                .build();
        workflowState = WorkflowState.builder()
                .room(room)
                .currentDraft("Draft slogan.")
                .reviewComments("Make it punchier.")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testToProviderFormat_FlatMessageArrayWithRoles() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.SYSTEM).content("System instructions").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").modelId("gpt-4-turbo").build(),
                CanonicalMessage.builder().senderType(SenderType.AI).content("Hello, how can I help?").build()
        );

        Object result = adapter.toProviderFormat(history, workflowState);
        assertNotNull(result);
        assertTrue(result instanceof OpenAiAdapter.OpenAiRequest);

        OpenAiAdapter.OpenAiRequest request = (OpenAiAdapter.OpenAiRequest) result;
        assertEquals("gpt-4-turbo", request.getModel());
        
        List<OpenAiAdapter.OpenAiMessage> messages = request.getMessages();
        // 1 state message + 3 history messages = 4 messages total
        assertEquals(4, messages.size());

        // First message is workflow state
        assertEquals("system", messages.get(0).getRole());
        assertTrue(messages.get(0).getContent().contains("Generate a marketing slogan."));
        
        // Second message is history system
        assertEquals("system", messages.get(1).getRole());
        assertEquals("System instructions", messages.get(1).getContent());

        // Third message is user
        assertEquals("user", messages.get(2).getRole());
        assertEquals("Hello", messages.get(2).getContent());

        // Fourth message is assistant
        assertEquals("assistant", messages.get(3).getRole());
        assertEquals("Hello, how can I help?", messages.get(3).getContent());
    }

    @Test
    void testToProviderFormat_EmptyHistoryNoState_ThrowsTranslationException() {
        assertThrows(TranslationException.class, () -> adapter.toProviderFormat(List.of(), null));
    }

    @Test
    void testFromProviderFormat_ValidResponse() {
        OpenAiAdapter.OpenAiResponse mockResponse = OpenAiAdapter.OpenAiResponse.builder()
                .id("chatcmpl-123")
                .object("chat.completion")
                .model("gpt-4")
                .choices(List.of(new OpenAiAdapter.OpenAiResponse.Choice(
                        0,
                        new OpenAiAdapter.OpenAiMessage("assistant", "Slogan: Think Fast."),
                        "stop"
                )))
                .build();

        CanonicalMessage message = adapter.fromProviderFormat(mockResponse);
        assertNotNull(message);
        assertEquals(SenderType.AI, message.getSenderType());
        assertEquals("Slogan: Think Fast.", message.getContent());
        assertEquals("gpt-4", message.getModelId());
    }

    @Test
    void testFromProviderFormat_JsonStringResponse() {
        String json = "{\n" +
                "  \"id\": \"chatcmpl-456\",\n" +
                "  \"object\": \"chat.completion\",\n" +
                "  \"model\": \"gpt-3.5-turbo\",\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"index\": 0,\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"Punchy Slogan here.\"\n" +
                "      },\n" +
                "      \"finish_reason\": \"stop\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        CanonicalMessage message = adapter.fromProviderFormat(json);
        assertNotNull(message);
        assertEquals("Punchy Slogan here.", message.getContent());
        assertEquals("gpt-3.5-turbo", message.getModelId());
    }

    @Test
    void testFromProviderFormat_InvalidJson_ThrowsTranslationException() {
        assertThrows(TranslationException.class, () -> adapter.fromProviderFormat("{ invalid }"));
    }
}
