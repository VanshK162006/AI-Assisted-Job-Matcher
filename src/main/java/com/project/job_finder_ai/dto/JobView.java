package com.project.job_finder_ai.dto;

import com.project.job_finder_ai.entity.Job;

import java.time.Instant;

public record JobView(Long id, String title, String company, String location,
                      String department,
                      String applyUrl,
                      Instant firstSeenAt,
                      Boolean passedFilter,
                      Instant notifiedAt) {

    public static JobView from(Job job){
        return new JobView(
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation(),
                job.getDepartment(),
                job.getApplyUrl(),
                job.getFirstSeenAt(),
                job.isPassedFilter(),
                job.getNotifiedAt()
        );
    }
}