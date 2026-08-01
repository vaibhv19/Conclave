package com.conclave.service;

import com.conclave.domain.*;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.domain.enums.SenderType;
import com.conclave.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class WorkflowStateServiceTest {

    @Autowired
    private WorkflowStateService workflowStateService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowStateRepository workflowStateRepository;

    @Autowired
    private CanonicalMessageRepository messageRepository;

    @MockBean
    private org.springframework.ai.ollama.OllamaChatModel ollamaChatModel;

    private Room room;
    private WorkflowState state;

    @BeforeEach
    void setUp() {
        // Setup mock Ollama ChatModel responses
        // We simulate a JSON response containing draft and comments from Llama 3 Janitor
        String mockResponseJson = "{\n" +
                "  \"currentDraft\": \"Compressed Slogan Draft\",\n" +
                "  \"reviewComments\": \"Compressed Review Comments\"\n" +
                "}";
        org.springframework.ai.chat.model.ChatResponse mockResponse = new org.springframework.ai.chat.model.ChatResponse(
                List.of(new org.springframework.ai.chat.model.Generation(mockResponseJson))
        );
        Mockito.when(ollamaChatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenReturn(mockResponse);

        User owner = User.builder()
                .email("janitor-owner@example.com")
                .name("Owner")
                .passwordHash("password")
                .build();
        owner = userRepository.save(owner);

        room = Room.builder()
                .name("Janitor Room")
                .objective("Test Janitor context compression")
                .owner(owner)
                .status(RoomStatus.INITIALIZED)
                .build();
        room = roomRepository.save(room);

        state = WorkflowState.builder()
                .room(room)
                .currentDraft("Initial draft content")
                .reviewComments("Initial review comments")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        state = workflowStateRepository.save(state);
    }

    @Test
    void testEvaluateAndCompressHistory_UnderThreshold_DoesNothing() {
        // Seed 5 messages
        for (int i = 0; i < 5; i++) {
            messageRepository.save(CanonicalMessage.builder()
                    .room(room)
                    .senderType(SenderType.USER)
                    .content("Message " + i)
                    .createdAt(LocalDateTime.now().plusSeconds(i))
                    .build());
        }

        workflowStateService.evaluateAndCompressHistory(room.getId());

        // Verify history length remains 5
        List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
        assertEquals(5, history.size());

        // Verify state is unchanged
        WorkflowState currentState = workflowStateService.getWorkflowState(room.getId());
        assertEquals("Initial draft content", currentState.getCurrentDraft());
    }

    @Test
    void testEvaluateAndCompressHistory_OverThreshold_CompressesAndPurges() {
        // Seed 11 messages
        // Message 0 will be the first message (context foundation)
        // Messages 1..8 will be middle messages (to be deleted)
        // Messages 9, 10 will be last 2 messages (short-term memory)
        for (int i = 0; i < 11; i++) {
            messageRepository.save(CanonicalMessage.builder()
                    .room(room)
                    .senderType(i % 2 == 0 ? SenderType.USER : SenderType.AI)
                    .content("Content " + i)
                    .createdAt(LocalDateTime.now().plusSeconds(i))
                    .build());
        }

        List<CanonicalMessage> initialHistory = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
        assertEquals(11, initialHistory.size());

        workflowStateService.evaluateAndCompressHistory(room.getId());

        // Verify history has exactly 3 messages remaining
        List<CanonicalMessage> finalHistory = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
        assertEquals(3, finalHistory.size());

        // Verify preserved message values
        assertEquals("Content 0", finalHistory.get(0).getContent());
        assertEquals("Content 9", finalHistory.get(1).getContent());
        assertEquals("Content 10", finalHistory.get(2).getContent());

        // Verify WorkflowState was updated
        WorkflowState currentState = workflowStateService.getWorkflowState(room.getId());
        assertNotNull(currentState.getCurrentDraft());
        assertEquals("Compressed Slogan Draft", currentState.getCurrentDraft());
        assertEquals("Compressed Review Comments", currentState.getReviewComments());
    }
}
