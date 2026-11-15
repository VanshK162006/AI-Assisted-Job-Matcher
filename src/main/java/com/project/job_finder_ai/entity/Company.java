package com.project.job_finder_ai.entity;

import com.project.job_finder_ai.enums.AtsType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"ats_type","board_slug"})
)
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(value = EnumType.STRING)
    @Column(name = "ats_type")
    private AtsType atsType;
    @Column(name = "board_slug")
    private String boardSlug;
    private String careersUrl;
    private boolean active=true;
    private Instant lastFetchedAt;
    private Instant createdAt=Instant.now();
}
