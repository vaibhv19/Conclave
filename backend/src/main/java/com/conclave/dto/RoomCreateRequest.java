package com.conclave.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCreateRequest {

    @NotBlank(message = "Room name is required")
    private String name;

    @NotBlank(message = "Room objective is required")
    private String objective;

    @NotEmpty(message = "At least one role assignment is required")
    @Valid
    private List<RoleAssignmentDTO> roleAssignments;
}
