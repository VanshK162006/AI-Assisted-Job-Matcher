package com.project.job_finder_ai.dto;

public record ScoringResult(int scored, int failed, int skipped, String note) {}