/*
Class: JobFilterService
It works is just take all the active companies Jobs from the database and each job
have a column field named as "passedFilter" which contains either true or false.

So, reFilterAll() check each and every job of active companies and mark their passedFilter
filter based on the decision given by JobFilter. Which matches the patterns of the Jobs.

At the last, reFilterAll() returns RefilterResult which is the summary dto it shows
how many jobs found, passed filter, failed,etc.....
 */

package com.project.job_finder_ai.service.directRepositoryConnection;

import com.project.job_finder_ai.dto.NotifyResult;
import com.project.job_finder_ai.dto.summary.RefilterResult;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.entity.JobMatch;
import com.project.job_finder_ai.repository.JobMatchRepository;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.dto.FilterDecision;
import com.project.job_finder_ai.service.TelegramNotificationService;
import com.project.job_finder_ai.service.filteringJobs.JobFilter;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobFilterService {

    private final JobFilter jobFilter;
    private final JobRepository jobRepository;

    public void markAll(List<Job> jobs) {
        List<Job> passedFilter=new ArrayList<>();

        for (Job job : jobs) {
            FilterDecision d = jobFilter.evaluate(
                    job.getTitle(), job.getLocation(), job.getDescriptionText());
            job.setPassedFilter(d.passed());
        }
        jobRepository.saveAll(jobs);
    }

    @Transactional
    public void markNotified(Collection<Long> matchIds) {
        Instant now = Instant.now();
        for (Job m : jobRepository.findAllById(matchIds)) {
            m.setNotifiedAt(now);   // no save() needed: the transaction writes the change
        }
    }

    @Transactional
    public RefilterResult reFilterAll() {
        List<Job> jobs = jobRepository.findByActiveTrueOrderByFirstSeenAtDesc();
        int passed = (int) jobs.stream()
                .filter(j -> Boolean.TRUE.equals(j.isPassedFilter())).count();
        return new RefilterResult(jobs.size(), passed);
    }

    public Long countToNotify(){
        return jobRepository.countToNotify();
    }
}