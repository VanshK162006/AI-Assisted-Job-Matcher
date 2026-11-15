package com.project.job_finder_ai.dto;

import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.entity.JobMatch;
import com.project.job_finder_ai.enums.ApplicationStatus;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public record JobMatchView(Long id, Long jobId, String title, String company, String location,
                           String applyUrl, int score, String reason,
                           Set<String> matchedSkills, Set<String> missingSkills,
                           ApplicationStatus status, Instant scoredAt) {

    public static JobMatchView from(JobMatch m) {
        Job j = m.getJob();
        return new JobMatchView(m.getId(), j.getId(), j.getTitle(), j.getCompany().getName(),
            j.getLocation(), j.getApplyUrl(), m.getScore(), m.getReason(),
            new LinkedHashSet<>(m.getMatchedSkills()), new LinkedHashSet<>(m.getMissingSkills()),
            m.getStatus(), m.getScoredAt());
    }
}