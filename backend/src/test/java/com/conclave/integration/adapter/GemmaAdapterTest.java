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

class GemmaAdapterTest {

    private GemmaAdapter adapter;
    private Room room;
    private WorkflowState workflowState;

    @BeforeEach
    void setUp() {
        adapter = new GemmaAdapter();
        room = Room.builder()
                .name("Gemma Room")
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
    void testToModelFormat_GemmaMessages() {
        List<CanonicalMessage> history = List.of(
                CanonicalMessage.builder().senderType(SenderType.USER).content("Hello").build(),
                CanonicalMessage.builder().senderType(SenderType.AI).content("Hello, how can I help?").build()
        );

        List<Message> result = adapter.toModelFormat(history, workflowState);
        assertNotNull(result);
        assertEquals(3, result.size());

        // First message is workflow state system message
        assertEquals(MessageType.SYSTEM, result.get(0).getMessageType());
        assertTrue(result.get(0).getContent().contains("Generate a marketing slogan."));

        // Second message is user
        assertEquals(MessageType.USER, result.get(1).getMessageType());
        assertEquals("Hello", result.get(1).getContent());

        // Third message is model response
        assertEquals(MessageType.ASSISTANT, result.get(2).getMessageType());
        assertEquals("Hello, how can I help?", result.get(2).getContent());
    }
}
