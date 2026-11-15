package com.project.job_finder_ai.entity;

import com.project.job_finder_ai.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "job_match")
@Getter
@Setter
@NoArgsConstructor
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    private int score;

    @Column(length = 1000)
    private String reason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_match_matched_skills", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "skill")
    private Set<String> matchedSkills = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_match_missing_skills", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "skill")
    private Set<String> missingSkills = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.NEW;

    private Instant scoredAt;
    private Instant statusUpdatedAt;
    private Instant notifiedAt;   // Day 9 fills this when a job is sent to Telegram
}