package com.conclave.domain;

import com.conclave.domain.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoomStatus status;

    @Column(name = "pipeline_sequence", columnDefinition = "TEXT")
    private String pipelineSequence;

    @Column(name = "current_pipeline_index")
    private Integer currentPipelineIndex;

    public java.util.List<String> getPipelineSequenceList() {
        if (pipelineSequence == null || pipelineSequence.trim().isEmpty()) {
            return java.util.List.of();
        }
        // split by comma, and trim each role name
        return java.util.Arrays.stream(pipelineSequence.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    public void setPipelineSequenceList(java.util.List<String> sequence) {
        if (sequence == null) {
            this.pipelineSequence = null;
        } else {
            this.pipelineSequence = String.join(",", sequence);
        }
    }
}
