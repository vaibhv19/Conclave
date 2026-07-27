package com.conclave.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStateDTO {
    private UUID id;
    private String currentDraft;
    private String reviewComments;
    private LocalDateTime lastUpdatedAt;
}
