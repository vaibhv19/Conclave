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

class ClaudeAdapterTest {

    private ClaudeAdapter adapter;
    private Room room;
    private WorkflowState workflowState;

    @BeforeEach
    void setUp() {
        adapter = new ClaudeAdapter();
        room = Room.builder()
                .name("Claude Room")
                .objective("Design a database schema.")
                .build();
        workflowState = WorkflowState.builder()
                .room(room)
                .currentDraft("No draft.")
                .reviewComments("No comments.")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testToProviderFormat_ValidAlternatingRolesAndSystemExtraction() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.SYSTEM).content("Perform well.").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").modelId("claude-3").build(),
                CanonicalMessage.builder().senderType(SenderType.AI).content("Hello!").build(),
                CanonicalMessage.builder().senderType(SenderType.SYSTEM).content("Another instruction.").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("Provide schema.").build()
        );

        Object result = adapter.toProviderFormat(history, workflowState);
        assertNotNull(result);
        assertTrue(result instanceof ClaudeAdapter.ClaudeRequest);

        ClaudeAdapter.ClaudeRequest request = (ClaudeAdapter.ClaudeRequest) result;
        assertEquals("claude-3", request.getModel());
        
        // System parameter should contain system messages + workflow state details
        String systemPrompt = request.getSystem();
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("Perform well."));
        assertTrue(systemPrompt.contains("Another instruction."));
        assertTrue(systemPrompt.contains("System Objective: Design a database schema."));

        // Messages array should contain only USER and AI messages, and should alternate
        List<ClaudeAdapter.ClaudeMessage> messages = request.getMessages();
        assertEquals(3, messages.size());
        
        assertEquals("user", messages.get(0).getRole());
        assertEquals("Hello", messages.get(0).getContent());

        assertEquals("assistant", messages.get(1).getRole());
        assertEquals("Hello!", messages.get(1).getContent());

        assertEquals("user", messages.get(2).getRole());
        assertEquals("Provide schema.", messages.get(2).getContent());
    }

    @Test
    void testToProviderFormat_NonAlternatingRoles_ThrowsTranslationException() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello again").build()
        );

        assertThrows(TranslationException.class, () -> adapter.toProviderFormat(history, null));
    }

    @Test
    void testToProviderFormat_StartingWithAssistant_ThrowsTranslationException() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.AI).content("Assistant first").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("User second").build()
        );

        assertThrows(TranslationException.class, () -> adapter.toProviderFormat(history, null));
    }

    @Test
    void testFromProviderFormat_ValidResponse() {
        ClaudeAdapter.ClaudeResponse mockResponse = ClaudeAdapter.ClaudeResponse.builder()
                .id("msg_123")
                .type("message")
                .model("claude-3")
                .content(List.of(new ClaudeAdapter.ClaudeResponse.ContentBlock("text", "Database schema: users, tables.")))
                .stopReason("end_turn")
                .build();

        CanonicalMessage message = adapter.fromProviderFormat(mockResponse);
        assertNotNull(message);
        assertEquals(SenderType.AI, message.getSenderType());
        assertEquals("Database schema: users, tables.", message.getContent());
        assertEquals("claude-3", message.getModelId());
    }

    @Test
    void testFromProviderFormat_JsonStringResponse() {
        String json = "{\n" +
                "  \"id\": \"msg_456\",\n" +
                "  \"type\": \"message\",\n" +
                "  \"model\": \"claude-3-opus\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"text\": \"Parsed Claude response.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        CanonicalMessage message = adapter.fromProviderFormat(json);
        assertNotNull(message);
        assertEquals("Parsed Claude response.", message.getContent());
        assertEquals("claude-3-opus", message.getModelId());
    }

    @Test
    void testFromProviderFormat_InvalidJson_ThrowsTranslationException() {
        assertThrows(TranslationException.class, () -> adapter.fromProviderFormat("{ invalid }"));
    }
}
