package com.conclave.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
class WebSocketSecurityTest {

    @Autowired
    private WebSocketAuthChannelInterceptor interceptor;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = new User("user@example.com", "password", List.of());
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mockUserDetails);
    }

    @Test
    void testPreSend_WithValidToken_AuthenticatesUser() {
        String token = "valid-token";
        when(jwtService.extractEmail(token)).thenReturn("user@example.com");
        when(jwtService.isTokenValid(token, mockUserDetails)).thenReturn(true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, Mockito.mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals("user@example.com", resultAccessor.getUser().getName());
    }

    @Test
    void testPreSend_WithInvalidToken_ThrowsException() {
        String token = "invalid-token";
        when(jwtService.extractEmail(token)).thenReturn("user@example.com");
        when(jwtService.isTokenValid(token, mockUserDetails)).thenReturn(false);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> {
            interceptor.preSend(message, Mockito.mock(MessageChannel.class));
        });
    }

    @Test
    void testPreSend_MissingHeader_ThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> {
            interceptor.preSend(message, Mockito.mock(MessageChannel.class));
        });
    }
}
