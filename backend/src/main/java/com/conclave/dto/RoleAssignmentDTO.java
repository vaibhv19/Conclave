package com.conclave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAssignmentDTO {

    @NotBlank(message = "Role name is required")
    private String roleName;

    @NotBlank(message = "Model ID is required")
    private String modelId;

    @NotBlank(message = "UI color hex code is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "UI color hex code must be a valid 6-digit hex code starting with # (e.g., #FFFFFF)")
    private String uiColorHex;
}
