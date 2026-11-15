package com.project.job_finder_ai.dto.candidate;

import jakarta.validation.constraints.*;

import java.util.List;

public record ProfileRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Email String email,
    @NotNull @DecimalMin("0.0") @DecimalMax("50.0") Double yearsOfExperience,
    @NotEmpty List<String> skills,
    @NotEmpty List<String> targetRoles,
    List<String> preferredLocations,
    @NotBlank @Size(max = 20000) String resumeText) {}