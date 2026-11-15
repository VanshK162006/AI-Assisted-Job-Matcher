package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.dto.JobMatchView;
import com.project.job_finder_ai.enums.ApplicationStatus;
import com.project.job_finder_ai.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final JobMatchService service;

    @GetMapping
    public List<JobMatchView> list(
            @RequestParam(defaultValue = "0") int minScore,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "20") int limit) {
        return service.list(minScore, status, limit);
    }

    @GetMapping("/{id}")
    public JobMatchView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    public JobMatchView updateStatus(@PathVariable Long id, @RequestParam ApplicationStatus value) {
        return service.updateStatus(id, value);
    }
}