package com.conclave.repository;

import com.conclave.domain.RoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {
    List<RoleAssignment> findByRoomId(UUID roomId);

    @Modifying
    @Query("DELETE FROM RoleAssignment r WHERE r.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") UUID roomId);
}
