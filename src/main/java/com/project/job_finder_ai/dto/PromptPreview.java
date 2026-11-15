package com.project.job_finder_ai.dto;

public record PromptPreview(
        String systemPrompt,
        String userPrompt,
        int userPromptCharacters) {}