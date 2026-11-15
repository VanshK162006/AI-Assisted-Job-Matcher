package com.project.job_finder_ai.dto;

import com.project.job_finder_ai.entity.FetchRun;
import com.project.job_finder_ai.enums.RunStatus;

import java.time.Instant;

public record FetchRunView(Long id, String company, RunStatus status,
                           Instant startedAt, Instant finishedAt,
                           int jobsFound, int newJobs, String errorMessage) {

    public static FetchRunView from(FetchRun r) {
        return new FetchRunView(r.getId(), r.getCompany().getName(), r.getRunStatus(),
            r.getStartedAt(), r.getFinishedAt(), r.getJobsFound(), r.getNewJobs(),
            r.getErrorMessage());
    }
}