package com.project.job_finder_ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne()
    @JoinColumn(name = "company_id")
    private Company company;
    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false,length = 500)
    private String title;
    private String location;
    private String department;
    private String employmentType;


    @Column(nullable = false,length = 1000)
    private String applyUrl;
    @Column(columnDefinition = "TEXT")
    private String descriptionText;
    private Instant postedAt;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private boolean active = true;
    private boolean passedFilter=false;
    private Long score;
    private Instant notifiedAt;
}
