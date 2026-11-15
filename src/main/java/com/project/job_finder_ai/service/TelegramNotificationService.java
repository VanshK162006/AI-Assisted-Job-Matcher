package com.project.job_finder_ai.service;

import com.project.job_finder_ai.config.TelegramProperties;
import com.project.job_finder_ai.dto.DigestMessage;
import com.project.job_finder_ai.dto.JobMatchView;
import com.project.job_finder_ai.dto.JobView;
import com.project.job_finder_ai.dto.NotifyResult;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.entity.JobMatch;
import com.project.job_finder_ai.enums.ApplicationStatus;
import com.project.job_finder_ai.repository.JobMatchRepository;
import com.project.job_finder_ai.service.directRepositoryConnection.JobFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    // keep this field order: the unit test builds the object in this order
    private final JobMatchRepository matchRepository;
    private final JobMatchService jobMatchService;
    private final DigestMessageFormatter formatter;
    private final TelegramSender sender;
    private final TelegramProperties props;
    @Autowired
    private JobFilterService jobFilterService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public NotifyResult notifyNewMatches() {
        if (!sender.isConfigured()) {
            return new NotifyResult(false, 0, 0, "Telegram is disabled or not configured");
        }
        if (!running.compareAndSet(false, true)) {
            return new NotifyResult(false, 0, 0, "A notification is already being sent");
        }
        try {
            return doNotify();
        } finally {
            running.set(false);
        }
    }

    public NotifyResult notifyNewMatches(List<Job> newJobs) {
        if (!sender.isConfigured()) {
            return new NotifyResult(false, 0, 0, "Telegram is disabled or not configured");
        }
        if (!running.compareAndSet(false, true)) {
            return new NotifyResult(false, 0, 0, "A notification is already being sent");
        }
        try {
            return doNotify(newJobs);
        } finally {
            running.set(false);
        }
    }

    private NotifyResult doNotify(List<Job> latestPassedFilterJob) {
        if (latestPassedFilterJob.isEmpty()) {
            return new NotifyResult(false, 0, 0, "Nothing new to send");
        }
        List<JobView> jobViewList = new java.util.ArrayList<>(latestPassedFilterJob.stream().map(
                (e) -> JobView.from(e)
        ).toList());

        for (JobView j : jobViewList) {
            if (j.notifiedAt() != null) {
                jobViewList.remove(j);
            }
        }
        long totalPending=jobFilterService.countToNotify()- latestPassedFilterJob.size();
        DigestMessage digest = formatter.formatJobs(jobViewList, totalPending);

        sender.send(digest.text());                              // throws if Telegram fails
        jobFilterService.markNotified(digest.includedMatchIds()); // only after success

        long waiting = Math.max(0, totalPending - digest.includedMatchIds().size());
        log.info("Telegram JobPassedFilter digest sent: {} matches, {} still waiting",
                digest.includedMatchIds().size(), 0);
        return new NotifyResult(true, digest.includedMatchIds().size(), waiting, "Sent");
    }

    private NotifyResult doNotify() {
        List<JobMatch> top = matchRepository.findToNotify(
                ApplicationStatus.NEW,
                props.getMinScore(),
                PageRequest.of(0, props.getMaxJobsPerMessage()));
        if (top.isEmpty()) {
            return new NotifyResult(false, 0, 0, "Nothing new to send");
        }
        long totalPending = matchRepository.countToNotify(
                ApplicationStatus.NEW, props.getMinScore());
        List<JobMatchView> views = top.stream().map(JobMatchView::from).toList();
        DigestMessage digest = formatter.format(views, totalPending);

        sender.send(digest.text());                              // throws if Telegram fails
        jobMatchService.markNotified(digest.includedMatchIds()); // only after success

        long waiting = Math.max(0, totalPending - digest.includedMatchIds().size());
        log.info("Telegram digest sent: {} matches, {} still waiting",
                digest.includedMatchIds().size(), waiting);
        return new NotifyResult(true, digest.includedMatchIds().size(), waiting, "Sent");
    }
}