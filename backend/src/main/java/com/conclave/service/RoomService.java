package com.conclave.service;

import com.conclave.domain.CanonicalMessage;
import com.conclave.domain.RoleAssignment;
import com.conclave.domain.Room;
import com.conclave.domain.User;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.dto.RoleAssignmentDTO;
import com.conclave.dto.RoomCreateRequest;
import com.conclave.dto.RoomResponse;
import com.conclave.dto.WorkflowStateDTO;
import com.conclave.exception.InvalidMappingException;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.exception.UnauthorizedAccessException;
import com.conclave.repository.RoleAssignmentRepository;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.WorkflowStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final WorkflowStateRepository workflowStateRepository;

    private static final Set<String> SUPPORTED_MODELS = Set.of("GEMINI_PRO", "FAKE_OPENAI", "FAKE_CLAUDE");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public RoomResponse createRoom(RoomCreateRequest request, User owner) {
        log.info("Creating new room '{}' for owner: {}", request.getName(), owner.getEmail());

        validateRoleAssignments(request.getRoleAssignments());

        // 1. Create and Save Room
        Room room = Room.builder()
                .owner(owner)
                .name(request.getName())
                .objective(request.getObjective())
                .status(RoomStatus.INITIALIZED)
                .build();
        Room savedRoom = roomRepository.save(room);

        // 2. Create and Save Role Assignments
        List<RoleAssignment> assignments = request.getRoleAssignments().stream()
                .map(dto -> RoleAssignment.builder()
                        .room(savedRoom)
                        .roleName(dto.getRoleName())
                        .modelId(dto.getModelId())
                        .uiColorHex(dto.getUiColorHex())
                        .build())
                .collect(Collectors.toList());
        List<RoleAssignment> savedAssignments = roleAssignmentRepository.saveAll(assignments);

        // 3. Create and Save WorkflowState
        WorkflowState workflowState = WorkflowState.builder()
                .room(savedRoom)
                .currentDraft("")
                .reviewComments("")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        WorkflowState savedState = workflowStateRepository.save(workflowState);

        log.info("Room created successfully with ID: {}", savedRoom.getId());
        return mapToRoomResponse(savedRoom, savedAssignments, savedState);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID roomId, User currentUser) {
        log.info("Fetching details for room: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        validateOwnership(room, currentUser);

        List<RoleAssignment> assignments = roleAssignmentRepository.findByRoomId(roomId);
        WorkflowState workflowState = workflowStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found for room ID: " + roomId));

        return mapToRoomResponse(room, assignments, workflowState);
    }

    public RoomResponse updateRoleAssignments(UUID roomId, List<RoleAssignmentDTO> newAssignments, User currentUser) {
        log.info("Updating role assignments for room: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        validateOwnership(room, currentUser);
        validateRoleAssignments(newAssignments);

        // Purge existing role assignments
        roleAssignmentRepository.deleteByRoomId(roomId);
        // Flush changes to database before saving new ones to avoid unique constraints violation
        roleAssignmentRepository.flush();

        // Create new role assignments
        List<RoleAssignment> assignments = newAssignments.stream()
                .map(dto -> RoleAssignment.builder()
                        .room(room)
                        .roleName(dto.getRoleName())
                        .modelId(dto.getModelId())
                        .uiColorHex(dto.getUiColorHex())
                        .build())
                .collect(Collectors.toList());
        List<RoleAssignment> savedAssignments = roleAssignmentRepository.saveAll(assignments);

        WorkflowState workflowState = workflowStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found for room ID: " + roomId));

        log.info("Role assignments updated successfully for room: {}", roomId);
        return mapToRoomResponse(room, savedAssignments, workflowState);
    }

    private void validateRoleAssignments(List<RoleAssignmentDTO> roleAssignments) {
        if (roleAssignments == null || roleAssignments.isEmpty()) {
            throw new InvalidMappingException("At least one role assignment is required");
        }

        Set<String> rolesSeen = new HashSet<>();

        for (RoleAssignmentDTO dto : roleAssignments) {
            // Check Supported Model
            if (!SUPPORTED_MODELS.contains(dto.getModelId())) {
                throw new InvalidMappingException("Model '" + dto.getModelId() + "' is not supported. Supported models: " + SUPPORTED_MODELS);
            }

            // Check Hex Color Code Format
            if (dto.getUiColorHex() == null || !HEX_COLOR_PATTERN.matcher(dto.getUiColorHex()).matches()) {
                throw new InvalidMappingException("Invalid UI color hex code format: " + dto.getUiColorHex());
            }

            // Check Unique Role Names (case-insensitive or exact matching, standard is case-sensitive uniqueness)
            if (!rolesSeen.add(dto.getRoleName())) {
                throw new InvalidMappingException("Duplicate role name '" + dto.getRoleName() + "' is not allowed in the same room");
            }
        }
    }

    private void validateOwnership(Room room, User currentUser) {
        if (!room.getOwner().getId().equals(currentUser.getId())) {
            log.warn("Access Denied: User {} is not the owner of room {}", currentUser.getEmail(), room.getId());
            throw new UnauthorizedAccessException("You do not have permission to access or modify this room");
        }
    }

    private RoomResponse mapToRoomResponse(Room room, List<RoleAssignment> assignments, WorkflowState state) {
        List<RoleAssignmentDTO> assignmentDTOs = assignments.stream()
                .map(a -> RoleAssignmentDTO.builder()
                        .roleName(a.getRoleName())
                        .modelId(a.getModelId())
                        .uiColorHex(a.getUiColorHex())
                        .build())
                .collect(Collectors.toList());

        WorkflowStateDTO stateDTO = WorkflowStateDTO.builder()
                .id(state.getId())
                .currentDraft(state.getCurrentDraft())
                .reviewComments(state.getReviewComments())
                .lastUpdatedAt(state.getLastUpdatedAt())
                .build();

        return RoomResponse.builder()
                .roomId(room.getId())
                .name(room.getName())
                .objective(room.getObjective())
                .status(room.getStatus())
                .roleAssignments(assignmentDTOs)
                .workflowState(stateDTO)
                .build();
    }
}
