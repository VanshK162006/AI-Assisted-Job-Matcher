package com.project.job_finder_ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "candidate_profile")
@Getter
@Setter
@NoArgsConstructor
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private double yearsOfExperience;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private Set<String> skills = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_target_roles", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "role")
    private Set<String> targetRoles = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_preferred_locations", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "location")
    private Set<String> preferredLocations = new LinkedHashSet<>();

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    private Instant updatedAt;
}