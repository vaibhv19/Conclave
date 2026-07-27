package com.conclave.controller;

import com.conclave.dto.RoleAssignmentDTO;
import com.conclave.dto.RoomCreateRequest;
import com.conclave.dto.RoomResponse;
import com.conclave.security.UserPrincipal;
import com.conclave.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody RoomCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("REST request to create room: {}", request.getName());
        RoomResponse response = roomService.createRoom(request, principal.getUser());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoom(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("REST request to get room: {}", id);
        RoomResponse response = roomService.getRoomById(id, principal.getUser());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/role-assignments")
    public ResponseEntity<RoomResponse> updateRoleAssignments(
            @PathVariable UUID id,
            @Valid @RequestBody List<RoleAssignmentDTO> request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("REST request to update role assignments for room: {}", id);
        RoomResponse response = roomService.updateRoleAssignments(id, request, principal.getUser());
        return ResponseEntity.ok(response);
    }
}
