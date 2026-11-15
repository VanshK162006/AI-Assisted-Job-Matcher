package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.dto.FetchRunView;
import com.project.job_finder_ai.service.directRepositoryConnection.FetchRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class FetchRunController {

    private final FetchRunService service;

    @GetMapping
    public List<FetchRunView> list(@RequestParam(required = false) Long companyId) {
        return service.list(companyId);
    }
}