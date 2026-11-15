package com.project.job_finder_ai.dto.lever;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeverPosting(String id, String text, String hostedUrl, String applyUrl, LeverCategories categories,
                           String descriptionPlain, Long createdAt) {
}
