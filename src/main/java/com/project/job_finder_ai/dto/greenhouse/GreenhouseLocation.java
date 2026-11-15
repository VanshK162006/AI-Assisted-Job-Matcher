package com.project.job_finder_ai.dto.greenhouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseLocation(String name) {}