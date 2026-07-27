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
import com.conclave.security.JwtService;
import com.conclave.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PipelineSequentialIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    private WorkflowStateRepository workflowStateRepository;

    @Autowired
    private CanonicalMessageRepository messageRepository;

    @Autowired
    private TokenUsageLogRepository tokenUsageLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private User owner;
    private User intruder;
    private String ownerToken;
    private String intruderToken;
    private Room room;

    @BeforeEach
    void setUp() {
        tokenUsageLogRepository.deleteAll();
        messageRepository.deleteAll();
        workflowStateRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        owner = User.builder()
                .email("owner@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("Owner User")
                .build();
        owner = userRepository.save(owner);

        intruder = User.builder()
                .email("intruder@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("Intruder User")
                .build();
        intruder = userRepository.save(intruder);

        ownerToken = jwtService.generateToken(new UserPrincipal(owner));
        intruderToken = jwtService.generateToken(new UserPrincipal(intruder));

        room = Room.builder()
                .name("Pipeline Integrator Room")
                .objective("Test complete end to end sequential auto advancement")
                .owner(owner)
                .status(RoomStatus.INITIALIZED)
                .build();
        room.setPipelineSequenceList(List.of("Lead-Writer", "Code-Critic"));
        room = roomRepository.save(room);

        // Assign Lead-Writer and Code-Critic to Fake Openai/Claude
        RoleAssignment writer = RoleAssignment.builder()
                .room(room)
                .roleName("Lead-Writer")
                .modelId(ModelId.FAKE_OPENAI.name())
                .uiColorHex("#ff0000")
                .build();
        roleAssignmentRepository.save(writer);

        RoleAssignment critic = RoleAssignment.builder()
                .room(room)
                .roleName("Code-Critic")
                .modelId(ModelId.FAKE_CLAUDE.name())
                .uiColorHex("#0000ff")
                .build();
        roleAssignmentRepository.save(critic);

        WorkflowState state = WorkflowState.builder()
                .room(room)
                .currentDraft("Initial draft")
                .reviewComments("Initial review comments")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        workflowStateRepository.save(state);
    }

    @AfterEach
    void tearDown() {
        tokenUsageLogRepository.deleteAll();
        messageRepository.deleteAll();
        workflowStateRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testSequentialAutoAdvanceFlow_CompletesAllSteps() throws Exception {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .roomId(room.getId())
                .content("Build features @Lead-Writer")
                .isIntervention(false)
                .build();

        mockMvc.perform(post("/api/chat/message")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        // Wait for async sequence to run completely (Lead-Writer finishes -> index advances -> Code-Critic runs)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CanonicalMessage> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(room.getId());
            // Expected history: User message + Lead-Writer response + Code-Critic response = 3 messages
            assertEquals(3, history.size());
            assertEquals(SenderType.USER, history.get(0).getSenderType());
            assertEquals(SenderType.AI, history.get(1).getSenderType());
            assertEquals("Lead-Writer", history.get(1).getRoleName());
            assertEquals(SenderType.AI, history.get(2).getSenderType());
            assertEquals("Code-Critic", history.get(2).getRoleName());
        });

        // Verify room final index matches end of sequence (index 1)
        Room dbRoom = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(RoomStatus.ACTIVE, dbRoom.getStatus());
        assertEquals(1, dbRoom.getCurrentPipelineIndex());
    }

    @Test
    void testPauseAndResumeEndpoints_RestrictsNonOwners() throws Exception {
        // Set room status to ACTIVE to allow pausing
        room.setStatus(RoomStatus.ACTIVE);
        roomRepository.save(room);

        // Intruder tries to pause -> 403 Forbidden / 401 Unauthorized depending on mapping
        mockMvc.perform(post("/api/chat/pipeline/pause")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("roomId", room.getId()))))
                .andExpect(status().isForbidden());

        // Owner pauses successfully
        mockMvc.perform(post("/api/chat/pipeline/pause")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("roomId", room.getId()))))
                .andExpect(status().isOk());

        Room dbRoom = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(RoomStatus.PAUSED, dbRoom.getStatus());

        // Intruder tries to resume -> 403
        mockMvc.perform(post("/api/chat/pipeline/resume")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("roomId", room.getId()))))
                .andExpect(status().isForbidden());

        // Owner resumes successfully
        mockMvc.perform(post("/api/chat/pipeline/resume")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("roomId", room.getId()))))
                .andExpect(status().isOk());

        dbRoom = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(RoomStatus.ACTIVE, dbRoom.getStatus());
    }

    @Test
    void testUserIntervention_TransitionsToPaused_AndBroadcastsSystemIntervention() throws Exception {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .roomId(room.getId())
                .content("Hold on, change course please.")
                .isIntervention(true)
                .build();

        mockMvc.perform(post("/api/chat/message")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        Room dbRoom = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(RoomStatus.PAUSED, dbRoom.getStatus());

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/room/" + room.getId()),
                any(com.conclave.dto.ws.SystemInterventionEvent.class)
        );
    }
}
