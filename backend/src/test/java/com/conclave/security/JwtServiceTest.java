package com.conclave.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "test-very-long-secret-key-that-is-at-least-256-bits-long-for-testing-purposes-only");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 10000L); // 10 seconds expiration

        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    @Test
    void testGenerateAndExtractEmail() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);

        String email = jwtService.extractEmail(token);
        assertEquals("test@example.com", email);
    }

    @Test
    void testTokenValidation() {
        String token = jwtService.generateToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));

        UserDetails otherUser = new User("other@example.com", "password", Collections.emptyList());
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void testTokenExpiration() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 100L); // 100 ms expiration
        String token = jwtService.generateToken(userDetails);
        
        Thread.sleep(150L);
        
        assertTrue(jwtService.isTokenExpired(token));
    }
}
