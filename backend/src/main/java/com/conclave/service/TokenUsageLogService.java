package com.conclave.service;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.Room;
import com.conclave.domain.TokenUsageLog;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.repository.CanonicalMessageRepository;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.TokenUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service to capture and persist LLM token usage metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenUsageLogService {

    private final TokenUsageLogRepository tokenUsageLogRepository;
    private final RoomRepository roomRepository;
    private final CanonicalMessageRepository messageRepository;

    /**
     * Records a single token usage log entry linked to a conversation turn.
     *
      * @param roomId           The room ID
     * @param messageId        The target CanonicalMessage ID
     * @param modelId          The model identifier string
     * @param promptTokens     The prompt tokens consumed
     * @param completionTokens The completion tokens generated
     * @param isMocked         Whether the generation was mocked
     * @return The persisted TokenUsageLog entry
     */
    @Transactional
    public TokenUsageLog logUsage(UUID roomId, UUID messageId, String modelId, int promptTokens, int completionTokens, boolean isMocked) {
        log.info("Logging token usage - Room: {}, Message: {}, Model: {}, Prompt: {}, Completion: {}, Mocked: {}",
                roomId, messageId, modelId, promptTokens, completionTokens, isMocked);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        CanonicalMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        TokenUsageLog usageLog = TokenUsageLog.builder()
                .room(room)
                .message(message)
                .modelId(modelId)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .isMocked(isMocked)
                .build();

        return tokenUsageLogRepository.save(usageLog);
    }
}
