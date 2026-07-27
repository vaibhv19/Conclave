package com.conclave.service;

import com.conclave.domain.Room;
import com.conclave.domain.User;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.exception.OrchestrationException;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.exception.UnauthorizedAccessException;
import com.conclave.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service implementation of PipelineManager handling pessimistic locking, transitions, and execution flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineManagerImpl implements PipelineManager {

    private final RoomRepository roomRepository;
    
    @Lazy
    private final MessageOrchestrator messageOrchestrator;

    @Override
    @Transactional
    public Room pausePipeline(UUID roomId, User requester) {
        log.info("Attempting to pause pipeline for room: {}, user: {}", roomId, requester.getEmail());

        // Acquire pessimistic write lock on Room to block concurrent updates
        Room room = roomRepository.findWithLockById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // Validate Ownership
        if (!room.getOwner().getId().equals(requester.getId())) {
            throw new UnauthorizedAccessException("Only the room owner can pause the pipeline");
        }

        // Validate State Transition
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new OrchestrationException("Cannot pause pipeline: Room status is not ACTIVE. Current status: " + room.getStatus());
        }

        room.setStatus(RoomStatus.PAUSED);
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public Room resumePipeline(UUID roomId, User requester) {
        log.info("Attempting to resume pipeline for room: {}, user: {}", roomId, requester.getEmail());

        // Acquire pessimistic write lock
        Room room = roomRepository.findWithLockById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // Validate Ownership
        if (!room.getOwner().getId().equals(requester.getId())) {
            throw new UnauthorizedAccessException("Only the room owner can resume the pipeline");
        }

        // Validate State Transition (PAUSED or INITIALIZED -> ACTIVE)
        if (room.getStatus() != RoomStatus.PAUSED && room.getStatus() != RoomStatus.INITIALIZED) {
            throw new OrchestrationException("Cannot resume pipeline: Room status is not PAUSED or INITIALIZED. Current status: " + room.getStatus());
        }

        room.setStatus(RoomStatus.ACTIVE);

        // Update pipeline index and trigger next step if configured
        List<String> sequence = room.getPipelineSequenceList();
        if (!sequence.isEmpty()) {
            int nextIndex = (room.getCurrentPipelineIndex() == null) ? 0 : room.getCurrentPipelineIndex() + 1;
            if (nextIndex < sequence.size()) {
                room.setCurrentPipelineIndex(nextIndex);
                room = roomRepository.save(room);

                String nextRole = sequence.get(nextIndex);
                log.info("Resuming pipeline at step index {} (role: {}). Triggering turn async.", nextIndex, nextRole);
                
                // Trigger async streaming execution for target role
                messageOrchestrator.executeStreamingTurn(roomId, nextRole, "Pipeline continuation step.");
            } else {
                log.info("Pipeline index {} is out of sequence bounds. No further steps to execute.", nextIndex);
                room = roomRepository.save(room);
            }
        } else {
            room = roomRepository.save(room);
        }

        return room;
    }
}
