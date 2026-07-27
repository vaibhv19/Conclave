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

class GeminiAdapterTest {

    private GeminiAdapter adapter;
    private Room room;
    private WorkflowState workflowState;

    @BeforeEach
    void setUp() {
        adapter = new GeminiAdapter();
        room = Room.builder()
                .name("Test Room")
                .objective("Write a beautiful essay on nature.")
                .build();
        workflowState = WorkflowState.builder()
                .room(room)
                .currentDraft("This is the nature draft.")
                .reviewComments("Add more descriptive adjectives.")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testToProviderFormat_ValidAlternatingRoles() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").build(),
                CanonicalMessage.builder().senderType(SenderType.AI).content("Hi, I can help.").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("Describe a forest.").build()
        );

        Object result = adapter.toProviderFormat(history, workflowState);
        assertNotNull(result);
        assertTrue(result instanceof GeminiAdapter.GeminiRequest);

        GeminiAdapter.GeminiRequest request = (GeminiAdapter.GeminiRequest) result;
        List<GeminiAdapter.Content> contents = request.getContents();
        assertEquals(3, contents.size());

        // Check first turn (should contain system prefix + message)
        GeminiAdapter.Content firstTurn = contents.get(0);
        assertEquals("user", firstTurn.getRole());
        String text = firstTurn.getParts().get(0).getText();
        assertTrue(text.contains("System Objective: Write a beautiful essay on nature."));
        assertTrue(text.contains("Current Draft: This is the nature draft."));
        assertTrue(text.contains("Review Comments: Add more descriptive adjectives."));
        assertTrue(text.endsWith("Hello"));

        // Check second turn
        GeminiAdapter.Content secondTurn = contents.get(1);
        assertEquals("model", secondTurn.getRole());
        assertEquals("Hi, I can help.", secondTurn.getParts().get(0).getText());

        // Check third turn
        GeminiAdapter.Content thirdTurn = contents.get(2);
        assertEquals("user", thirdTurn.getRole());
        assertEquals("Describe a forest.", thirdTurn.getParts().get(0).getText());
    }

    @Test
    void testToProviderFormat_SystemMessagesPrependToFirstUser() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.SYSTEM).content("System instruction 1").build(),
                CanonicalMessage.builder().senderType(SenderType.SYSTEM).content("System instruction 2").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("User prompt").build()
        );

        Object result = adapter.toProviderFormat(history, null); // state is null
        GeminiAdapter.GeminiRequest request = (GeminiAdapter.GeminiRequest) result;
        List<GeminiAdapter.Content> contents = request.getContents();
        assertEquals(1, contents.size());

        String text = contents.get(0).getParts().get(0).getText();
        assertTrue(text.contains("System instruction 1"));
        assertTrue(text.contains("System instruction 2"));
        assertTrue(text.endsWith("User prompt"));
    }

    @Test
    void testToProviderFormat_InvalidNonAlternatingRoles_ThrowsTranslationException() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("Double hello").build()
        );

        assertThrows(TranslationException.class, () -> adapter.toProviderFormat(history, null));
    }

    @Test
    void testToProviderFormat_InvalidStartingWithAi_ThrowsTranslationException() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.AI).content("AI response first").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("User response second").build()
        );

        assertThrows(TranslationException.class, () -> adapter.toProviderFormat(history, null));
    }

    @Test
    void testToProviderFormat_SystemMessageInMiddle_ThrowsTranslationException() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").build(),
                CanonicalMessage.builder().senderType(SenderType.SYSTEM).content("System message").build()
        );

        assertThrows(TranslationException.class, () -> adapter.toProviderFormat(history, null));
    }

    @Test
    void testFromProviderFormat_ValidResponse() {
        GeminiAdapter.GeminiResponse mockResponse = new GeminiAdapter.GeminiResponse(
                List.of(new GeminiAdapter.GeminiResponse.Candidate(
                        new GeminiAdapter.Content("model", List.of(new GeminiAdapter.Part("Nature is green."))),
                        "STOP"
                ))
        );

        CanonicalMessage message = adapter.fromProviderFormat(mockResponse);
        assertNotNull(message);
        assertEquals(SenderType.AI, message.getSenderType());
        assertEquals("Nature is green.", message.getContent());
    }

    @Test
    void testFromProviderFormat_JsonStringResponse() {
        String json = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"role\": \"model\",\n" +
                "        \"parts\": [\n" +
                "          { \"text\": \"Deserialized Nature.\" }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        CanonicalMessage message = adapter.fromProviderFormat(json);
        assertNotNull(message);
        assertEquals("Deserialized Nature.", message.getContent());
    }

    @Test
    void testFromProviderFormat_InvalidJson_ThrowsTranslationException() {
        String invalidJson = "{ invalid: json }";
        assertThrows(TranslationException.class, () -> adapter.fromProviderFormat(invalidJson));
    }
}
