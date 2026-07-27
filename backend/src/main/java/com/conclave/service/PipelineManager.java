package com.conclave.service;

import com.conclave.domain.Room;
import com.conclave.domain.User;
import java.util.UUID;

/**
 * Service managing state transitions and thread-safe pessimistic locks of sequentially executed model pipelines.
 */
public interface PipelineManager {

    /**
     * Pauses the active room pipeline execution flow.
     * Only valid transition is from RoomStatus.ACTIVE -> RoomStatus.PAUSED.
     *
     * @param roomId    The target Room ID to lock and pause
     * @param requester The User requesting the pause
     * @return The updated Room entity
     */
    Room pausePipeline(UUID roomId, User requester);

    /**
     * Resumes a paused or initialized room pipeline execution flow.
     * Valid transitions are RoomStatus.PAUSED -> RoomStatus.ACTIVE, or RoomStatus.INITIALIZED -> RoomStatus.ACTIVE.
     * Increments the current pipeline pointer and triggers the next model step.
     *
     * @param roomId    The target Room ID to lock and resume
     * @param requester The User requesting the resume
     * @return The updated Room entity
     */
    Room resumePipeline(UUID roomId, User requester);
}
