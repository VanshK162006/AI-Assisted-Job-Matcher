package com.project.job_finder_ai.dto.candidate;

import com.project.job_finder_ai.entity.CandidateProfile;

import java.time.Instant;
import java.util.Set;

public record ProfileView(Long id, String fullName, String email, double yearsOfExperience,
                          Set<String> skills, Set<String> targetRoles,
                          Set<String> preferredLocations, String resumeText,
                          Instant updatedAt) {

    public static ProfileView from(CandidateProfile p) {
        return new ProfileView(p.getId(), p.getFullName(), p.getEmail(),
            p.getYearsOfExperience(), p.getSkills(), p.getTargetRoles(),
            p.getPreferredLocations(), p.getResumeText(), p.getUpdatedAt());
    }
}