package com.project.job_finder_ai.dto.greenhouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseJob(
    Long id,
    String title,
    @JsonProperty("absolute_url") String absoluteUrl,
    GreenhouseLocation location,
    String content,
    @JsonProperty("updated_at") String updatedAt) {}
