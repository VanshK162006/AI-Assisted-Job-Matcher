package com.project.job_finder_ai.dto.Ashby;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AshbyJob(
    String id,
    String title,
    String location,
    String department,
    String employmentType,
    String jobUrl,
    String applyUrl,
    String descriptionPlain,
    String publishedAt) {}