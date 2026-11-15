package com.project.job_finder_ai.dto.greenhouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GreenhouseResponse(List<GreenhouseJob> jobs) {}
