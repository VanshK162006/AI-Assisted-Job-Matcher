package com.project.job_finder_ai.dto.lever;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeverCategories(
        String location,
        String team,
        String department,
        String commitment) {
}
