package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.Room;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.SenderType;
import com.conclave.exception.TranslationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlamaAdapterTest {

    private LlamaAdapter adapter;
    private Room room;
    private WorkflowState workflowState;

    @BeforeEach
    void setUp() {
        adapter = new LlamaAdapter();
        room = Room.builder()
                .name("Llama Room")
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
    void testToModelFormat_MapsToSpringAiMessages() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.SYSTEM).content("System instructions").build(),
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").build(),
                CanonicalMessage.builder().senderType(SenderType.AI).content("Hello, how can I help?").build()
        );

        List<Message> result = adapter.toModelFormat(history, workflowState);
        assertNotNull(result);
        // 1 state message + 3 history messages = 4 messages total
        assertEquals(4, result.size());

        // First message is workflow state
        assertEquals(MessageType.SYSTEM, result.get(0).getMessageType());
        assertTrue(result.get(0).getContent().contains("Generate a marketing slogan."));

        // Second message is history system
        assertEquals(MessageType.SYSTEM, result.get(1).getMessageType());
        assertEquals("System instructions", result.get(1).getContent());

        // Third message is user
        assertEquals(MessageType.USER, result.get(2).getMessageType());
        assertEquals("Hello", result.get(2).getContent());

        // Fourth message is assistant
        assertEquals(MessageType.ASSISTANT, result.get(3).getMessageType());
        assertEquals("Hello, how can I help?", result.get(3).getContent());
    }

    @Test
    void testToModelFormat_EmptyHistoryNoState_ReturnsEmpty() {
        List<Message> result = adapter.toModelFormat(List.of(), null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFromModelFormat_ValidResponse() {
        CanonicalMessage message = adapter.fromModelFormat("Slogan: Think Fast.");
        assertNotNull(message);
        assertEquals(SenderType.AI, message.getSenderType());
        assertEquals("Slogan: Think Fast.", message.getContent());
    }
}
