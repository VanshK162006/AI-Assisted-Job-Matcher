/*
This class yet contains just the logic to fetch Jobs from the Ats Fetcher of all the
active companies like Razorpay,OpenAi, Anthropic,etc...

It will return just the summary of how many companies tried, how many failed, new jobs found,
total jobs found,etc.... i.e. FetchAllResult Dto
 */

package com.project.job_finder_ai.service;

import com.project.job_finder_ai.dto.NotifyResult;
import com.project.job_finder_ai.dto.ScoringResult;
import com.project.job_finder_ai.dto.summary.FetchAllResult;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.entity.FetchRun;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.entity.JobMatch;
import com.project.job_finder_ai.enums.RunStatus;
import com.project.job_finder_ai.exception.PipelineBusyException;
import com.project.job_finder_ai.repository.JobMatchRepository;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.service.directRepositoryConnection.CompanyServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Not;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPipelineService {

    private final CompanyServices companyService;
    private final CompanyFetchServices companyFetchService;
    private final MatchingRunService aiServices;
    private final TelegramNotificationService notificationService;
    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${pipeline.pause-millis:500}")
    private long pauseMillis;
    @Value("${telegram.max-jobs-per-message}")
    private long maxMessage;

    /**
     * It simply fetches all the Active company and its blug from your database. If you
     * stored only 3 companies then it will find only Active out of those 3.
     * <p>
     * It runs for each loop for those active companies and with the help of each company.atsType
     * that fetchAndStore() method is called.
     * <p>
     * fetchAndStore() first identifies the AtsType from the fetcherRegistry Mapper
     * and look its matching one. It's matching is a class either GreenHouseFetcher,
     * LeverFetcher,etc.. and then it will call the fetch() method of those classes.
     *
     * @return It returns summary of trial, failure, job found, new jobs, reason for failure
     */
    public FetchAllResult runAll() {
        if (!running.compareAndSet(false, true)) {
            throw new PipelineBusyException();
        }
        try {
            List<Company> companies = companyService.listActive();
            log.info("Pipeline started for {} companies", companies.size());

            int failed = 0, found = 0, created = 0;
            List<String> failures = new ArrayList<>();

            for (Company company : companies) {
                FetchRun run = companyFetchService.fetchJobsOfCompany(company);
                if (run.getRunStatus() == RunStatus.SUCCESS) {
                    found += run.getJobsFound();
                    created += run.getNewJobs();
                } else {
                    failed++;
                    failures.add(company.getName() + ": " + run.getErrorMessage());
                }
                pause();
            }
            ScoringResult scoring = scoreSafely();
            NotifyResult notification = notifySafely();

            log.info("Pipeline finished: tried {}, failed {}, found {}, new {}, scored {}, sent {}",
                    companies.size(), failed, found, created, scoring.scored(), notification.matchesSent());
            return new FetchAllResult(companies.size(), failed, found, created, failures,
                    scoring, notification);
        } finally {
            running.set(false);
        }
    }

    public NotifyResult notifyNewJobsPassedYourFilter() {
        List<Job> jobs = jobRepository.findByPassedFilterTrueAndActiveTrueOrderByPostedAtDesc(PageRequest.of(0, (int) maxMessage));
        List<Job> notifiedJobMatchJobs = jobMatchRepository.findNotifiedJobMatch().stream().map(
                JobMatch::getJob
        ).toList();
        jobs.removeIf(notifiedJobMatchJobs::contains);
        return notificationService.notifyNewMatches(jobs);
    }

    private ScoringResult scoreSafely() {
        try {
            return aiServices.scorePending();
        } catch (PipelineBusyException e) {
            return new ScoringResult(0, 0, 0, "Scoring already running: skipped");
        } catch (Exception e) {
            log.warn("Scoring step failed: {}", e.getMessage());
            return new ScoringResult(0, 0, 0, "Scoring step failed: " + e.getMessage());
        }
    }

    private void pause() {
        try {
            Thread.sleep(pauseMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // A Telegram problem must never spoil the fetch and scoring results
    private NotifyResult notifySafely() {
        try {
            return notificationService.notifyNewMatches();
        } catch (Exception e) {
            log.warn("Telegram step failed: {}", e.getMessage());
            return new NotifyResult(false, 0, 0, "Telegram failed: " + e.getMessage());
        }
    }
}