package com.conclave.integration.adapter;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.Room;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.SenderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MistralAdapterTest {

    private MistralAdapter adapter;
    private Room room;
    private WorkflowState workflowState;

    @BeforeEach
    void setUp() {
        adapter = new MistralAdapter();
        room = Room.builder()
                .name("Mistral Room")
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
    void testToModelFormat_PrependsSystemToFirstUserMessage() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").build(),
                CanonicalMessage.builder().senderType(SenderType.AI).content("Hello, how can I help?").build()
        );

        List<Message> result = adapter.toModelFormat(history, workflowState);
        assertNotNull(result);
        // Should only produce 2 messages (No SystemMessage generated)
        assertEquals(2, result.size());

        // First message is user message prepended with system prompt
        assertEquals(MessageType.USER, result.get(0).getMessageType());
        assertTrue(result.get(0).getContent().contains("System Objective: Generate a marketing slogan."));
        assertTrue(result.get(0).getContent().contains("Hello"));

        // Second message is assistant message
        assertEquals(MessageType.ASSISTANT, result.get(1).getMessageType());
        assertEquals("Hello, how can I help?", result.get(1).getContent());
    }

    @Test
    void testToModelFormat_EmptyHistoryPrependsSystemToUserMessage() {
        List<Message> result = adapter.toModelFormat(List.of(), workflowState);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(MessageType.USER, result.get(0).getMessageType());
        assertTrue(result.get(0).getContent().contains("System Objective: Generate a marketing slogan."));
    }
}
