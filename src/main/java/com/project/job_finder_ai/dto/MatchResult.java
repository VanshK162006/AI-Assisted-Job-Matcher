package com.project.job_finder_ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record MatchResult(
    @JsonPropertyDescription("Fit score from 0 (no fit) to 100 (excellent fit)")
    Long score,

    @JsonPropertyDescription("At most two or three short sentences explaining the score")
    String reason,

    @JsonPropertyDescription("Skills that appear in BOTH the candidate profile and the job")
    List<String> matchedSkills,

    @JsonPropertyDescription("Important skills the job asks for that the candidate lacks (max 5)")
    List<String> missingSkills) {}