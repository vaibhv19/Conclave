package com.conclave.repository;

import com.conclave.domain.TokenUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TokenUsageLogRepository extends JpaRepository<TokenUsageLog, UUID> {
    List<TokenUsageLog> findByRoomId(UUID roomId);
}
