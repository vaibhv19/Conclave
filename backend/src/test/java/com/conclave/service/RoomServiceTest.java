package com.conclave.service;

import com.conclave.domain.RoleAssignment;
import com.conclave.domain.Room;
import com.conclave.domain.User;
import com.conclave.domain.WorkflowState;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.dto.RoleAssignmentDTO;
import com.conclave.dto.RoomCreateRequest;
import com.conclave.dto.RoomResponse;
import com.conclave.exception.InvalidMappingException;
import com.conclave.exception.ResourceNotFoundException;
import com.conclave.exception.UnauthorizedAccessException;
import com.conclave.repository.RoleAssignmentRepository;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.WorkflowStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;
    @Mock
    private WorkflowStateRepository workflowStateRepository;

    @InjectMocks
    private RoomService roomService;

    private User owner;
    private User otherUser;
    private RoomCreateRequest createRequest;
    private Room room;
    private WorkflowState workflowState;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .name("Owner")
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .name("Other User")
                .build();

        RoleAssignmentDTO assignment = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("GEMINI_PRO")
                .uiColorHex("#FF5733")
                .build();

        createRequest = RoomCreateRequest.builder()
                .name("Test Room")
                .objective("Test Objective")
                .roleAssignments(Collections.singletonList(assignment))
                .build();

        room = Room.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Test Room")
                .objective("Test Objective")
                .status(RoomStatus.INITIALIZED)
                .build();

        workflowState = WorkflowState.builder()
                .id(UUID.randomUUID())
                .room(room)
                .currentDraft("")
                .reviewComments("")
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateRoom_Success() {
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roleAssignmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowStateRepository.save(any(WorkflowState.class))).thenReturn(workflowState);

        RoomResponse response = roomService.createRoom(createRequest, owner);

        assertNotNull(response);
        assertEquals(room.getId(), response.getRoomId());
        assertEquals("Test Room", response.getName());
        assertEquals(RoomStatus.INITIALIZED, response.getStatus());
        assertEquals(1, response.getRoleAssignments().size());
        assertEquals("Writer", response.getRoleAssignments().get(0).getRoleName());
        assertEquals("", response.getWorkflowState().getCurrentDraft());

        verify(roomRepository, times(1)).save(any(Room.class));
        verify(roleAssignmentRepository, times(1)).saveAll(anyList());
        verify(workflowStateRepository, times(1)).save(any(WorkflowState.class));
    }

    @Test
    void testCreateRoom_InvalidModel_ThrowsException() {
        createRequest.getRoleAssignments().get(0).setModelId("UNSUPPORTED_MODEL");

        assertThrows(InvalidMappingException.class, () -> roomService.createRoom(createRequest, owner));

        verifyNoInteractions(roomRepository, roleAssignmentRepository, workflowStateRepository);
    }

    @Test
    void testCreateRoom_InvalidColor_ThrowsException() {
        createRequest.getRoleAssignments().get(0).setUiColorHex("blue");

        assertThrows(InvalidMappingException.class, () -> roomService.createRoom(createRequest, owner));

        verifyNoInteractions(roomRepository, roleAssignmentRepository, workflowStateRepository);
    }

    @Test
    void testCreateRoom_DuplicateRoles_ThrowsException() {
        RoleAssignmentDTO assignment1 = RoleAssignmentDTO.builder()
                .roleName("Writer")
                .modelId("GEMINI_PRO")
                .uiColorHex("#FF5733")
                .build();

        RoleAssignmentDTO assignment2 = RoleAssignmentDTO.builder()
                .roleName("Writer") // Duplicate
                .modelId("FAKE_OPENAI")
                .uiColorHex("#00FF00")
                .build();

        createRequest.setRoleAssignments(Arrays.asList(assignment1, assignment2));

        assertThrows(InvalidMappingException.class, () -> roomService.createRoom(createRequest, owner));

        verifyNoInteractions(roomRepository, roleAssignmentRepository, workflowStateRepository);
    }

    @Test
    void testGetRoomById_Success() {
        UUID roomId = room.getId();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roleAssignmentRepository.findByRoomId(roomId)).thenReturn(Collections.emptyList());
        when(workflowStateRepository.findByRoomId(roomId)).thenReturn(Optional.of(workflowState));

        RoomResponse response = roomService.getRoomById(roomId, owner);

        assertNotNull(response);
        assertEquals(roomId, response.getRoomId());
        verify(roomRepository, times(1)).findById(roomId);
    }

    @Test
    void testGetRoomById_Unauthorized_ThrowsException() {
        UUID roomId = room.getId();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThrows(UnauthorizedAccessException.class, () -> roomService.getRoomById(roomId, otherUser));

        verify(roomRepository, times(1)).findById(roomId);
        verifyNoMoreInteractions(roleAssignmentRepository, workflowStateRepository);
    }

    @Test
    void testGetRoomById_NotFound_ThrowsException() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomById(roomId, owner));

        verify(roomRepository, times(1)).findById(roomId);
        verifyNoMoreInteractions(roleAssignmentRepository, workflowStateRepository);
    }

    @Test
    void testUpdateRoleAssignments_Success() {
        UUID roomId = room.getId();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        doNothing().when(roleAssignmentRepository).deleteByRoomId(roomId);
        doNothing().when(roleAssignmentRepository).flush();
        when(roleAssignmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowStateRepository.findByRoomId(roomId)).thenReturn(Optional.of(workflowState));

        RoleAssignmentDTO newAssignment = RoleAssignmentDTO.builder()
                .roleName("Reviewer")
                .modelId("FAKE_CLAUDE")
                .uiColorHex("#123456")
                .build();

        RoomResponse response = roomService.updateRoleAssignments(roomId, Collections.singletonList(newAssignment), owner);

        assertNotNull(response);
        assertEquals(1, response.getRoleAssignments().size());
        assertEquals("Reviewer", response.getRoleAssignments().get(0).getRoleName());

        verify(roleAssignmentRepository, times(1)).deleteByRoomId(roomId);
        verify(roleAssignmentRepository, times(1)).saveAll(anyList());
    }
}
