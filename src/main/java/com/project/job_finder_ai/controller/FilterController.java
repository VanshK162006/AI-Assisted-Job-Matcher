package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.config.FilterProperties;
import com.project.job_finder_ai.dto.FilterCheckRequest;
import com.project.job_finder_ai.dto.FilterDecision;
import com.project.job_finder_ai.service.filteringJobs.JobFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/filter")
@RequiredArgsConstructor
public class FilterController {

    private final FilterProperties properties;
    private final JobFilter jobFilter;

    @GetMapping("/rules")
    public FilterProperties rules() {
        return properties;
    }

    @PostMapping("/check")
    public FilterDecision check(@RequestBody FilterCheckRequest request) {
        return jobFilter.evaluate(request.title(), request.location(), request.description());
    }
}