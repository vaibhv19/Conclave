package com.conclave.controller;

import com.conclave.domain.*;
import com.conclave.domain.enums.ModelId;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.domain.enums.SenderType;
import com.conclave.dto.ChatMessageRequest;
import com.conclave.dto.ws.ContentChunkEvent;
import com.conclave.dto.ws.TurnCompletedEvent;
import com.conclave.dto.ws.TurnStartedEvent;
import com.conclave.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Bypass HTTP security filters for clean API invocation
@ActiveProfiles("dev")
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Room room;
    private User owner;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        tokenUsageLogRepository.deleteAll();
        messageRepository.deleteAll();
        workflowStateRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .email("chat-owner@example.com")
                .name("Owner")
                .passwordHash("password")
                .build();
        owner = userRepository.save(owner);

        room = Room.builder()
                .name("Chat Room")
                .objective("Test chat streaming")
                .owner(owner)
                .status(RoomStatus.INITIALIZED)
                .build();
        room = roomRepository.save(room);

        // Add Role Assignment for Lead-Writer -> FAKE_OPENAI
        RoleAssignment assignment = RoleAssignment.builder()
                .room(room)
                .roleName("Lead-Writer")
                .modelId(ModelId.FAKE_OPENAI.name())
                .uiColorHex("#4287f5")
                .build();
        roleAssignmentRepository.save(assignment);

        // Add WorkflowState
        WorkflowState state = WorkflowState.builder()
                .room(room)
                .currentDraft("Initial draft content")
                .reviewComments("No comments")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        workflowStateRepository.save(state);
    }

    @Test
    void testPostMessage_WithMention_Returns202AndStreamsAiTurn() throws Exception {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .roomId(room.getId())
                .content("Hello @Lead-Writer write some slogans.")
                .isIntervention(false)
                .build();

        // 1. Invoke REST API
        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Verify user message was persisted immediately
        List<CanonicalMessage> userMessages = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
        assertFalse(userMessages.isEmpty());
        assertEquals("Hello @Lead-Writer write some slogans.", userMessages.get(0).getContent());
        assertEquals(SenderType.USER, userMessages.get(0).getSenderType());

        // 2. Wait for async processing and broadcast events
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify started event broadcast
            verify(messagingTemplate, atLeastOnce()).convertAndSend(
                    eq("/topic/room/" + room.getId()),
                    any(TurnStartedEvent.class)
            );

            // Verify content chunk event broadcast
            verify(messagingTemplate, atLeastOnce()).convertAndSend(
                    eq("/topic/room/" + room.getId()),
                    any(ContentChunkEvent.class)
            );

            // Verify completed event broadcast
            verify(messagingTemplate, atLeastOnce()).convertAndSend(
                    eq("/topic/room/" + room.getId()),
                    any(TurnCompletedEvent.class)
            );
        });

        // Verify AI message was saved in history
        List<CanonicalMessage> finalHistory = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
        assertEquals(2, finalHistory.size());
        assertEquals(SenderType.AI, finalHistory.get(1).getSenderType());
        assertEquals("Lead-Writer", finalHistory.get(1).getRoleName());
        assertEquals(ModelId.FAKE_OPENAI.name(), finalHistory.get(1).getModelId());
    }
}
