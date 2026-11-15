package com.project.job_finder_ai.service.util;

import com.project.job_finder_ai.dto.summary.FetchAllResult;
import com.project.job_finder_ai.exception.PipelineBusyException;
import com.project.job_finder_ai.service.JobPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PipelineScheduler {

    private final JobPipelineService pipeline;

    @Scheduled(cron = "${pipeline.cron}", zone = "${pipeline.zone:Asia/Kolkata}")
    public void scheduledRun() {
        try {
            FetchAllResult r = pipeline.runAll();
            log.info("Scheduled run done: {} new jobs, {} scored, {} sent",
                r.newJobs(), r.scoring().scored(), r.notification().matchesSent());
        } catch (PipelineBusyException e) {
            log.info("Scheduled run skipped: another run is in progress");
        }
    }
}