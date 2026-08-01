package com.conclave.controller;

import com.conclave.domain.User;
import com.conclave.dto.RoleAssignmentDTO;
import com.conclave.dto.RoomCreateRequest;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.UserRepository;
import com.conclave.security.JwtService;
import com.conclave.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        roomRepository.deleteAll();
        userRepository.deleteAll();

        userA = User.builder()
                .email("usera@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("User A")
                .build();
        userA = userRepository.save(userA);

        userB = User.builder()
                .email("userb@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("User B")
                .build();
        userB = userRepository.save(userB);

        tokenA = jwtService.generateToken(new UserPrincipal(userA));
        tokenB = jwtService.generateToken(new UserPrincipal(userB));
    }

    @Test
    void testCreateRoom_Success() throws Exception {
        RoleAssignmentDTO assignment = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("LLAMA3")
                .uiColorHex("#00FF00")
                .build();

        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("Consensus Room")
                .objective("Draft PRD")
                .roleAssignments(Collections.singletonList(assignment))
                .build();

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId", notNullValue()))
                .andExpect(jsonPath("$.name").value("Consensus Room"))
                .andExpect(jsonPath("$.roleAssignments", hasSize(1)))
                .andExpect(jsonPath("$.workflowState.currentDraft").value(""));
    }

    @Test
    void testCreateRoom_InvalidColor_BadRequest() throws Exception {
        RoleAssignmentDTO assignment = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("LLAMA3")
                .uiColorHex("#123") // Invalid color hex
                .build();

        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("Consensus Room")
                .objective("Draft PRD")
                .roleAssignments(Collections.singletonList(assignment))
                .build();

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateRoom_DuplicateRoles_BadRequest() throws Exception {
        RoleAssignmentDTO assignment1 = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("LLAMA3")
                .uiColorHex("#00FF00")
                .build();

        RoleAssignmentDTO assignment2 = RoleAssignmentDTO.builder()
                .roleName("Writer") // Duplicate
                .modelId("GEMMA")
                .uiColorHex("#FF0000")
                .build();

        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("Consensus Room")
                .objective("Draft PRD")
                .roleAssignments(Arrays.asList(assignment1, assignment2))
                .build();

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate role name 'Writer' is not allowed in the same room"));
    }

    @Test
    void testGetRoomDetail_OwnerAccess_Success() throws Exception {
        RoleAssignmentDTO assignment = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("LLAMA3")
                .uiColorHex("#00FF00")
                .build();

        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("A's Room")
                .objective("Write code")
                .roleAssignments(Collections.singletonList(assignment))
                .build();

        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String roomId = objectMapper.readTree(result.getResponse().getContentAsString()).get("roomId").asText();

        mockMvc.perform(get("/api/rooms/" + roomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("A's Room"));
    }

    @Test
    void testGetRoomDetail_NonOwnerAccess_Forbidden() throws Exception {
        RoleAssignmentDTO assignment = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("LLAMA3")
                .uiColorHex("#00FF00")
                .build();

        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("A's Room")
                .objective("Write code")
                .roleAssignments(Collections.singletonList(assignment))
                .build();

        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String roomId = objectMapper.readTree(result.getResponse().getContentAsString()).get("roomId").asText();

        mockMvc.perform(get("/api/rooms/" + roomId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access or modify this room"));
    }

    @Test
    void testUpdateRoleAssignments_Success() throws Exception {
        RoleAssignmentDTO assignment = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("LLAMA3")
                .uiColorHex("#00FF00")
                .build();

        RoomCreateRequest request = RoomCreateRequest.builder()
                .name("A's Room")
                .objective("Write code")
                .roleAssignments(Collections.singletonList(assignment))
                .build();

        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String roomId = objectMapper.readTree(result.getResponse().getContentAsString()).get("roomId").asText();

        RoleAssignmentDTO updatedAssignment = RoleAssignmentDTO.builder()
                .roleName("Reviewer")
                .modelId("GEMMA")
                .uiColorHex("#0000FF")
                .build();

        mockMvc.perform(put("/api/rooms/" + roomId + "/role-assignments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(updatedAssignment))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleAssignments", hasSize(1)))
                .andExpect(jsonPath("$.roleAssignments[0].roleName").value("Reviewer"))
                .andExpect(jsonPath("$.roleAssignments[0].modelId").value("GEMMA"));
    }
}
