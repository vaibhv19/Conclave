package com.conclave.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Interceptor that intercepts WebSocket channel messages to authenticate users using JWT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.debug("WebSocket CONNECT frame received. Authorization header: {}", authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String email = jwtService.extractEmail(token);
                    if (email != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        if (jwtService.isTokenValid(token, userDetails)) {
                            StompHeaderAccessor mutableAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                            if (mutableAccessor == null || !mutableAccessor.isMutable()) {
                                mutableAccessor = StompHeaderAccessor.wrap(message);
                            }

                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                            mutableAccessor.setUser(authentication);
                            log.info("WebSocket successfully authenticated user: {}", email);
                            return org.springframework.messaging.support.MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
                        } else {
                            log.warn("WebSocket CONNECT rejected: Invalid token for user {}", email);
                            throw new IllegalArgumentException("Invalid JWT token");
                        }
                    } else {
                        log.warn("WebSocket CONNECT rejected: Subject/email could not be extracted from token");
                        throw new IllegalArgumentException("Invalid JWT token structure");
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication exception", e);
                    throw new IllegalArgumentException("Authentication failed: " + e.getMessage(), e);
                }
            } else {
                log.warn("WebSocket CONNECT rejected: Missing or invalid Authorization header format");
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
        }

        return message;
    }
}
