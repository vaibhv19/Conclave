package com.conclave.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
class WebSocketConfigTest {

    @Autowired(required = false)
    private WebSocketConfig webSocketConfig;

    @Autowired(required = false)
    private DelegatingWebSocketMessageBrokerConfiguration delegatingConfiguration;

    @Test
    void testWebSocketConfigBeansExist() {
        assertNotNull(webSocketConfig, "WebSocketConfig bean should be registered");
        assertNotNull(delegatingConfiguration, "DelegatingWebSocketMessageBrokerConfiguration bean should be registered");
    }
}
