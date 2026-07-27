package com.conclave.controller;

import com.conclave.domain.*;
import com.conclave.domain.enums.SenderType;
import com.conclave.dto.ChatMessageRequest;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.repository.CanonicalMessageRepository;
import com.conclave.repository.RoomRepository;
import com.conclave.service.WorkflowStateService;
import com.conclave.util.MentionParser;
import com.conclave.security.UserPrincipal;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.dto.ws.SystemInterventionEvent;
import com.conclave.service.MessageOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

/**
 * Controller to handle incoming user chat messages asynchronously and stream AI agent responses.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RoomRepository roomRepository;
    private final CanonicalMessageRepository messageRepository;
    private final WorkflowStateService workflowStateService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageOrchestrator messageOrchestrator;
    private final com.conclave.service.PipelineManager pipelineManager;

    /**
     * Endpoint to receive a user message. Validates the request, persists the user message,
     * and initiates the asynchronous AI execution turn if a mention is found, returning immediately.
     *
     * @param request The chat message request DTO
     * @return 202 Accepted HTTP response
     */
    @PostMapping("/message")
    @Transactional
    public ResponseEntity<Void> postMessage(@RequestBody ChatMessageRequest request) {
        log.info("Received POST /api/chat/message for room: {}", request.getRoomId());

        if (request.getRoomId() == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Lock room pessimistic-style to prevent race conditions during state transitions
        Room room = roomRepository.findWithLockById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));

        // Save User message synchronously
        CanonicalMessage userMessage = CanonicalMessage.builder()
                .room(room)
                .senderType(SenderType.USER)
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .isMocked(false)
                .build();
        messageRepository.save(userMessage);

        if (request.isIntervention()) {
            log.info("User intervention detected. Pausing room {} and regenerating draft.", room.getId());
            
            // Halt sequential model chain execution by transitioning to PAUSED
            room.setStatus(RoomStatus.PAUSED);
            roomRepository.save(room);

            // Rebuild context draft/comments based on user intervention feedback
            workflowStateService.evaluateAndCompressHistory(room.getId());

            // Load updated workflow state context summaries
            WorkflowState updatedState = workflowStateService.getWorkflowState(room.getId());
            String draft = updatedState != null ? updatedState.getCurrentDraft() : "";
            String comments = updatedState != null ? updatedState.getReviewComments() : "";

            // Broadcast SYSTEM_INTERVENTION event
            SystemInterventionEvent interventionEvent = new SystemInterventionEvent(
                    "User feedback received: " + request.getContent(),
                    draft,
                    comments
            );
            messagingTemplate.convertAndSend("/topic/room/" + room.getId(), interventionEvent);

        } else {
            // Check if there is an AI mention in the message
            Optional<String> mentionOpt = MentionParser.extractMention(request.getContent());
            if (mentionOpt.isPresent()) {
                String mention = mentionOpt.get();

                // If room status is INITIALIZED and sequence is configured, transition to ACTIVE
                if (room.getStatus() == RoomStatus.INITIALIZED) {
                    room.setStatus(RoomStatus.ACTIVE);
                    room.setCurrentPipelineIndex(0);
                    roomRepository.save(room);
                }

                log.info("Parsed mention: @{}. Triggering streaming turn asynchronously.", mention);
                messageOrchestrator.executeStreamingTurn(room.getId(), mention, request.getContent());
            }
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/pipeline/pause")
    public ResponseEntity<Map<String, String>> pausePipeline(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (body == null || !body.containsKey("roomId") || principal == null) {
            return ResponseEntity.badRequest().build();
        }
        UUID roomId = UUID.fromString(body.get("roomId"));
        Room room = pipelineManager.pausePipeline(roomId, principal.getUser());
        return ResponseEntity.ok(Map.of("status", room.getStatus().name()));
    }

    @PostMapping("/pipeline/resume")
    public ResponseEntity<Map<String, String>> resumePipeline(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (body == null || !body.containsKey("roomId") || principal == null) {
            return ResponseEntity.badRequest().build();
        }
        UUID roomId = UUID.fromString(body.get("roomId"));
        Room room = pipelineManager.resumePipeline(roomId, principal.getUser());
        return ResponseEntity.ok(Map.of("status", room.getStatus().name()));
    }
}
