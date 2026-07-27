package com.conclave.repository;

import com.conclave.domain.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStateRepository extends JpaRepository<WorkflowState, UUID> {
    Optional<WorkflowState> findByRoomId(UUID roomId);
}
