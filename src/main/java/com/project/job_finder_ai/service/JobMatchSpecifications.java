package com.project.job_finder_ai.service;

import com.project.job_finder_ai.entity.JobMatch;
import com.project.job_finder_ai.enums.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

public final class JobMatchSpecifications {

    private JobMatchSpecifications() {}

    public static Specification<JobMatch> minScore(int min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Integer>get("score"), min);
    }

    public static Specification<JobMatch> status(ApplicationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}