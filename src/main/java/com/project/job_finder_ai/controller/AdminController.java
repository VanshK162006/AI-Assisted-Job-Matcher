package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.dto.NotifyResult;
import com.project.job_finder_ai.dto.ScoringResult;
import com.project.job_finder_ai.dto.summary.FetchAllResult;
import com.project.job_finder_ai.dto.FetchRunView;
import com.project.job_finder_ai.dto.summary.RefilterResult;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.service.*;
import com.project.job_finder_ai.service.directRepositoryConnection.JobFilterService;
import com.project.job_finder_ai.service.directRepositoryConnection.CompanyServices;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JobPipelineService pipeline;
    private final CompanyServices companyService;
    private final CompanyFetchServices companyFetchService;
    private final JobFilterService jobFilterService;
    private final MatchingRunService matchingRunService;

    private final TelegramSender telegramSender;
    private final TelegramNotificationService notificationService;

    @PostMapping("/run")
    public FetchAllResult runAll() {
        return pipeline.runAll();
    }

    @PostMapping("/run/{companyId}")
    public FetchRunView runOne(@PathVariable Long companyId) {
        Company company = companyService.getById(companyId);
        return FetchRunView.from(companyFetchService.fetchJobsOfCompany(company));
    }

    @PostMapping("/reFilter")
    public RefilterResult reFilter() {
        return jobFilterService.reFilterAll();
    }

    @PostMapping("/score-pending")
    public ScoringResult scorePending() {
        return matchingRunService.scorePending();
    }

    @PostMapping("/telegram-test")
    public Map<String, String> telegramTest() {
        telegramSender.send("✅ <b>Job Finder</b> is connected to Telegram.");
        return Map.of("result", "Test message sent. Check your Telegram.");
    }

    @PostMapping("/notify")
    public NotifyResult notifyNow() {
        return notificationService.notifyNewMatches();
    }

    @PostMapping("/notifyNewJobsPassedFilter")
    public NotifyResult notifyNewJobsPassedFilter() {
        return pipeline.notifyNewJobsPassedYourFilter();
    }

}