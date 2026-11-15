package com.project.job_finder_ai.dto;

import java.time.Instant;

public record NormalizedJob(
    String externalId,
    String title,
    String location,
    String department,
    String employmentType,
    String applyUrl,
    String descriptionText,
    Instant postedAt) {}