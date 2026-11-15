package com.project.job_finder_ai.dto.Ashby;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AshbyResponse(List<AshbyJob> jobs) {}