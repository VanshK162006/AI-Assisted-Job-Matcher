package com.project.job_finder_ai.dto;

import com.project.job_finder_ai.enums.AtsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompanyRequest(
        @NotBlank
        String name,
        @NotNull
        AtsType atsType,
        @NotBlank
        String boardSlug,
        @NotBlank
        String careersUrl
) {
}
