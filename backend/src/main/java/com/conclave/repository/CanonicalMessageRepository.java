package com.conclave.repository;

import com.conclave.domain.CanonicalMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CanonicalMessageRepository extends JpaRepository<CanonicalMessage, UUID> {
    List<CanonicalMessage> findByRoomIdOrderByCreatedAtAsc(UUID roomId);
}
