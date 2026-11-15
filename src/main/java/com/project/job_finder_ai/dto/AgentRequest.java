package com.project.job_finder_ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AgentRequest(String conversationId, @NotBlank String message) {}

