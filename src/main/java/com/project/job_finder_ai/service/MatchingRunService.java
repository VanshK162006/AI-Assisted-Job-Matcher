package com.project.job_finder_ai.service;

import com.project.job_finder_ai.dto.MatchResult;
import com.project.job_finder_ai.dto.ScoringResult;
import com.project.job_finder_ai.entity.CandidateProfile;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.exception.PipelineBusyException;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.service.ai.JobMatchingServices;
import com.project.job_finder_ai.service.directRepositoryConnection.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingRunService {

    // keep this field order: the unit test builds the object in this order
    private final CandidateProfileService profileService;
    private final JobRepository jobRepository;
    private final JobMatchingServices matchingService;
    private final JobMatchService jobMatchService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public ScoringResult scorePending() {
        if (!running.compareAndSet(false, true)) {
            throw new PipelineBusyException("Scoring is already in progress");
        }
        try {
            return doScore();
        } finally {
            running.set(false);
        }
    }

    private ScoringResult doScore() {
        Optional<CandidateProfile> profile = profileService.find();
        if (profile.isEmpty()) {
            return new ScoringResult(0, 0, 0, "No profile saved yet: scoring skipped");
        }

        List<Job> pending = jobRepository.findPendingForScoringAndPassedFilterTrue(
            PageRequest.of(0, 20));
        if (pending.isEmpty()) {
            return new ScoringResult(0, 0, 0, "Nothing to score");
        }

        int scored = 0, failed = 0, skipped = 0, streak = 0;
        String note = "OK";

        for (Job job : pending) {
            try {
                MatchResult result = matchingService.scoreJobById(job.getId(),profile.get().getEmail());
                jobMatchService.save(job, result);
                scored++;
                streak = 0;
            } catch (DataIntegrityViolationException e) {
                skipped++;   // another run already saved a card for this job
            } catch (Exception e) {
                failed++;
                streak++;
                log.warn("Scoring failed for job {}: {}", job.getId(), e.getMessage());
                if (streak >= 3) {
                    note = "Stopped early after " + streak + " failures in a row";
                    break;
                }
            }
            pause();
        }

        log.info("Scoring finished: scored {}, failed {}, skipped {}", scored, failed, skipped);
        return new ScoringResult(scored, failed, skipped, note);
    }

    private void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}