package com.conclave.service;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.Room;
import com.conclave.domain.TokenUsageLog;
import com.conclave.domain.User;
import com.conclave.domain.enums.SenderType;
import com.conclave.repository.CanonicalMessageRepository;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.TokenUsageLogRepository;
import com.conclave.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TokenUsageLogServiceTest {

    @Autowired
    private TokenUsageLogService tokenUsageLogService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CanonicalMessageRepository messageRepository;

    @Autowired
    private TokenUsageLogRepository tokenUsageLogRepository;

    private Room room;
    private CanonicalMessage message;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .email("testowner@example.com")
                .name("Owner")
                .passwordHash("hash")
                .build();
        owner = userRepository.save(owner);

        room = Room.builder()
                .name("Logging Room")
                .objective("Test logging")
                .owner(owner)
                .status(com.conclave.domain.enums.RoomStatus.INITIALIZED)
                .build();
        room = roomRepository.save(room);

        message = CanonicalMessage.builder()
                .room(room)
                .senderType(SenderType.USER)
                .content("Slogan query")
                .createdAt(LocalDateTime.now())
                .build();
        message = messageRepository.save(message);
    }

    @Test
    void testLogUsage_PersistsRecordCorrectly() {
        TokenUsageLog logEntry = tokenUsageLogService.logUsage(
                room.getId(),
                message.getId(),
                "LLAMA3",
                10,
                20,
                false
        );

        assertNotNull(logEntry);
        assertNotNull(logEntry.getId());
        assertEquals("LLAMA3", logEntry.getModelId());
        assertEquals(10, logEntry.getPromptTokens());
        assertEquals(20, logEntry.getCompletionTokens());
        assertFalse(logEntry.getIsMocked());
        assertEquals(room.getId(), logEntry.getRoom().getId());
        assertEquals(message.getId(), logEntry.getMessage().getId());

        // Verify lookup from repo
        List<TokenUsageLog> logs = tokenUsageLogRepository.findByRoomId(room.getId());
        assertEquals(1, logs.size());
        assertEquals(logEntry.getId(), logs.get(0).getId());
    }
}
