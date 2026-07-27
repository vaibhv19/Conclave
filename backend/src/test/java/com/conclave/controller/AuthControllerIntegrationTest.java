package com.conclave.controller;

import com.conclave.domain.User;
import com.conclave.dto.UserLoginRequest;
import com.conclave.dto.UserRegisterRequest;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.UserRepository;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .name("New User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.user.name").value("New User"));
    }

    @Test
    void testRegisterUser_DuplicateEmail_Conflict() throws Exception {
        User existingUser = User.builder()
                .email("duplicate@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("Existing User")
                .build();
        userRepository.save(existingUser);

        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("duplicate@example.com")
                .password("newpassword")
                .name("Duplicate User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email Conflict"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void testRegisterUser_ValidationFailure() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("invalid-email")
                .password("short")
                .name("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginUser_Success() throws Exception {
        User user = User.builder()
                .email("login@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("Login User")
                .build();
        userRepository.save(user);

        UserLoginRequest request = UserLoginRequest.builder()
                .email("login@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("login@example.com"));
    }

    @Test
    void testLoginUser_InvalidCredentials_Unauthorized() throws Exception {
        UserLoginRequest request = UserLoginRequest.builder()
                .email("nonexistent@example.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRestrictedRoutes_AccessControl() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isForbidden());
    }
}
