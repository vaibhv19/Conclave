package com.conclave.service;

import com.conclave.domain.Room;
import com.conclave.domain.User;
import com.conclave.domain.enums.RoomStatus;
import com.conclave.exception.OrchestrationException;
import com.conclave.exception.UnauthorizedAccessException;
import com.conclave.repository.RoomRepository;
import com.conclave.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class PipelineManagerTest {

    @Autowired
    private PipelineManager pipelineManager;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User otherUser;
    private Room room;

    @BeforeEach
    void setUp() {
        roomRepository.deleteAll();
        userRepository.deleteAll();

        owner = User.builder()
                .email("owner@example.com")
                .name("Owner")
                .passwordHash("password")
                .build();
        owner = userRepository.save(owner);

        otherUser = User.builder()
                .email("other@example.com")
                .name("Other User")
                .passwordHash("password")
                .build();
        otherUser = userRepository.save(otherUser);

        room = Room.builder()
                .name("Pipeline Control Room")
                .objective("Testing transitions")
                .owner(owner)
                .status(RoomStatus.INITIALIZED)
                .build();
        room.setPipelineSequenceList(List.of("Lead-Writer", "Code-Critic"));
        room = roomRepository.save(room);
    }

    @Test
    void testPausePipeline_TransitionsFromActiveToPaused() {
        room.setStatus(RoomStatus.ACTIVE);
        room = roomRepository.save(room);

        Room pausedRoom = pipelineManager.pausePipeline(room.getId(), owner);

        assertNotNull(pausedRoom);
        assertEquals(RoomStatus.PAUSED, pausedRoom.getStatus());

        Room dbRoom = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(RoomStatus.PAUSED, dbRoom.getStatus());
    }

    @Test
    void testPausePipeline_ThrowsException_IfRoomIsNotActive() {
        room.setStatus(RoomStatus.INITIALIZED);
        room = roomRepository.save(room);

        assertThrows(OrchestrationException.class, () -> 
            pipelineManager.pausePipeline(room.getId(), owner)
        );
    }

    @Test
    void testPausePipeline_ThrowsException_IfUserIsNotOwner() {
        room.setStatus(RoomStatus.ACTIVE);
        room = roomRepository.save(room);

        assertThrows(UnauthorizedAccessException.class, () -> 
            pipelineManager.pausePipeline(room.getId(), otherUser)
        );
    }

    @Test
    void testResumePipeline_TransitionsFromPausedToActive_AndIncrementsIndex() {
        room.setStatus(RoomStatus.PAUSED);
        room.setCurrentPipelineIndex(0); // completed index 0
        room = roomRepository.save(room);

        Room resumedRoom = pipelineManager.resumePipeline(room.getId(), owner);

        assertNotNull(resumedRoom);
        assertEquals(RoomStatus.ACTIVE, resumedRoom.getStatus());
        assertEquals(1, resumedRoom.getCurrentPipelineIndex());
    }

    @Test
    void testResumePipeline_TransitionsFromInitializedToActive_AndStartsAtIndexZero() {
        room.setStatus(RoomStatus.INITIALIZED);
        room.setCurrentPipelineIndex(null);
        room = roomRepository.save(room);

        Room resumedRoom = pipelineManager.resumePipeline(room.getId(), owner);

        assertNotNull(resumedRoom);
        assertEquals(RoomStatus.ACTIVE, resumedRoom.getStatus());
        assertEquals(0, resumedRoom.getCurrentPipelineIndex());
    }

    @Test
    void testResumePipeline_ThrowsException_IfUserIsNotOwner() {
        room.setStatus(RoomStatus.PAUSED);
        room = roomRepository.save(room);

        assertThrows(UnauthorizedAccessException.class, () -> 
            pipelineManager.resumePipeline(room.getId(), otherUser)
        );
    }

    @Test
    void testResumePipeline_ThrowsException_IfRoomIsNotPausedOrInitialized() {
        room.setStatus(RoomStatus.ACTIVE);
        room = roomRepository.save(room);

        assertThrows(OrchestrationException.class, () -> 
            pipelineManager.resumePipeline(room.getId(), owner)
        );
    }
}
