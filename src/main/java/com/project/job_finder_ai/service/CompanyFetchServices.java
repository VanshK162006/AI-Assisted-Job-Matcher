/*
It contains the logic for fetching the Jobs of a particular company from the Ats Fetcher
only. Then it returns the summary of the company like how many jobs tried, how many new
jobs found,etc....

 */

package com.project.job_finder_ai.service;

import com.project.job_finder_ai.dto.NormalizedJob;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.entity.FetchRun;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.enums.RunStatus;
import com.project.job_finder_ai.repository.CompanyRepository;
import com.project.job_finder_ai.repository.FetchRunRepository;
import com.project.job_finder_ai.service.AtsFetchers.FetcherRegistry;
import com.project.job_finder_ai.service.AtsFetchers.JobFetcher;
import com.project.job_finder_ai.service.directRepositoryConnection.JobIngestionServices;
import com.project.job_finder_ai.service.directRepositoryConnection.JobFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompanyFetchServices {

    private final FetcherRegistry registry;
    private final JobIngestionServices ingestionServices;
    private final FetchRunRepository fetchRunRepository;
    private final CompanyRepository companyRepository;
    private final JobFilterService jobFilterService;

    public FetchRun fetchJobsOfCompany(Company company) {
        FetchRun diary = new FetchRun();
        diary.setCompany(company);

        diary.setStartedAt(Instant.now());

        try {
            JobFetcher fetcher = registry.getFetcher(company.getAtsType());
            //All jobs of mentioned company from the API
            List<NormalizedJob> normalizedJobs = fetcher.fetch(company);

            //Found only new jobs and add them to database
            List<Job> latestJob = ingestionServices.latestJobs(company, normalizedJobs);
            jobFilterService.markAll(latestJob);

            diary.setRunStatus(RunStatus.SUCCESS);
            diary.setJobsFound(normalizedJobs.size());
            diary.setNewJobs(latestJob.size());

            company.setLastFetchedAt(Instant.now());
            companyRepository.save(company);

            log.info("{}: found {}, new {}", company.getName(), normalizedJobs.size(), latestJob.size());
        } catch (Exception e) {
            diary.setRunStatus(RunStatus.FAILED);
            diary.setErrorMessage(describe(e));
            log.warn("{} failed: {}", company.getName(), e.getMessage());
        }
        diary.setFinishedAt(Instant.now());
        FetchRun result= fetchRunRepository.save(diary);


        return result;
    }

    private String describe(Exception e) {
        String text = e.getClass().getSimpleName() + ": " + e.getMessage();
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }
}

