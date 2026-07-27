package com.conclave.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.UUID;

@Entity
@Table(
    name = "role_assignments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "role_name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "model_id", nullable = false, length = 100)
    private String modelId;

    @Column(name = "ui_color_hex", nullable = false, length = 7)
    private String uiColorHex;
}
