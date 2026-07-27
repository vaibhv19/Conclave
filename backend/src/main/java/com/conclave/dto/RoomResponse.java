package com.conclave.dto;

import com.conclave.domain.enums.RoomStatus;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private UUID roomId;
    private String name;
    private String objective;
    private RoomStatus status;
    private List<RoleAssignmentDTO> roleAssignments;
    private WorkflowStateDTO workflowState;
}
