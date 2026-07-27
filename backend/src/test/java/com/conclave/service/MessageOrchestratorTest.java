package com.conclave.service;

import com.conclave.domain.*;
import com.conclave.domain.enums.ModelId;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.domain.enums.SenderType;
import com.conclave.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MessageOrchestratorTest {

    @Autowired
    private MessageOrchestrator messageOrchestrator;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    private WorkflowStateRepository workflowStateRepository;

    @Autowired
    private CanonicalMessageRepository messageRepository;

    @Autowired
    private TokenUsageLogRepository tokenUsageLogRepository;

    private Room room;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .email("orchestrator-owner@example.com")
                .name("Owner")
                .passwordHash("password")
                .build();
        owner = userRepository.save(owner);

        room = Room.builder()
                .name("Orchestration Room")
                .objective("Test orchestration engine")
                .owner(owner)
                .status(RoomStatus.INITIALIZED)
                .build();
        room = roomRepository.save(room);

        // Add Role Assignment for Lead-Writer
        RoleAssignment assignment = RoleAssignment.builder()
                .room(room)
                .roleName("Lead-Writer")
                .modelId(ModelId.FAKE_OPENAI.name())
                .uiColorHex("#FF5733")
                .build();
        roleAssignmentRepository.save(assignment);

        // Add WorkflowState
        WorkflowState state = WorkflowState.builder()
                .room(room)
                .currentDraft("Initial draft")
                .reviewComments("No comments")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        workflowStateRepository.save(state);
    }

    @Test
    void testProcessUserTurn_OrchestratesExecutionRoundTrip() {
        CanonicalMessage response = messageOrchestrator.processUserTurn(
                room.getId(),
                "@Lead-Writer please write a slogan review."
        );

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Lead-Writer", response.getRoleName());
        assertEquals(ModelId.FAKE_OPENAI.name(), response.getModelId());
        assertEquals(SenderType.AI, response.getSenderType());
        assertTrue(response.getIsMocked());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("OpenAI Analysis Review"));

        // Verify history includes two messages: User + AI
        List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
        assertEquals(2, history.size());
        assertEquals(SenderType.USER, history.get(0).getSenderType());
        assertEquals(SenderType.AI, history.get(1).getSenderType());

        // Verify token usage logged
        List<TokenUsageLog> logs = tokenUsageLogRepository.findByRoomId(room.getId());
        assertEquals(1, logs.size());
        assertEquals(response.getId(), logs.get(0).getMessage().getId());
        assertTrue(logs.get(0).getPromptTokens() > 0);
        assertTrue(logs.get(0).getCompletionTokens() > 0);
    }
}
