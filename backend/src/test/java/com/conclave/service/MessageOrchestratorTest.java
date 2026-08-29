package com.conclave.service;

import com.conclave.domain.*;
import com.conclave.domain.enums.ModelId;
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

    @MockBean
    private org.springframework.ai.ollama.OllamaChatModel ollamaChatModel;

    private Room room;

    @BeforeEach
    void setUp() {
        // Setup mock Ollama ChatModel responses
        org.springframework.ai.chat.model.ChatResponse mockResponse = new org.springframework.ai.chat.model.ChatResponse(
                List.of(new org.springframework.ai.chat.model.Generation("### Llama Analysis Review\nOptimized structure."))
        );
        Mockito.when(ollamaChatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenReturn(mockResponse);
        Mockito.when(ollamaChatModel.stream(any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenReturn(reactor.core.publisher.Flux.just(mockResponse));

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

        // Add Role Assignment for Lead-Writer mapped to local model LLAMA3
        RoleAssignment assignment = RoleAssignment.builder()
                .room(room)
                .roleName("Lead-Writer")
                .modelId(ModelId.LLAMA3.name())
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
    void testExecuteStreamingTurnAsync_OrchestratesExecutionRoundTrip() {
        messageOrchestrator.executeStreamingTurnAsync(
                room.getId(),
                "Lead-Writer",
                "Please write a slogan review."
        );

        // Verify history includes the generated AI message
        List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
        assertFalse(history.isEmpty());
        CanonicalMessage response = history.get(history.size() - 1);
        assertEquals("Lead-Writer", response.getRoleName());
        assertEquals(ModelId.LLAMA3.name(), response.getModelId());
        assertEquals(SenderType.AI, response.getSenderType());
        assertFalse(response.getIsMocked());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("Llama Analysis Review"));

        // Verify token usage logged
        List<TokenUsageLog> logs = tokenUsageLogRepository.findByRoomId(room.getId());
        assertEquals(1, logs.size());
        assertEquals(response.getId(), logs.get(0).getMessage().getId());
        assertTrue(logs.get(0).getPromptTokens() > 0);
        assertTrue(logs.get(0).getCompletionTokens() > 0);
    }
}
