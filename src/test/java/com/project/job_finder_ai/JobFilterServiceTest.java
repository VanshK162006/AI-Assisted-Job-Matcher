package com.project.job_finder_ai;

import com.project.job_finder_ai.dto.FilterDecision;
import com.project.job_finder_ai.dto.summary.RefilterResult;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.service.directRepositoryConnection.JobFilterService;
import com.project.job_finder_ai.service.filteringJobs.JobFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobFilterServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobFilter jobFilter;
    @InjectMocks
    private JobFilterService jobFilterService;

    private Job job(long id, String title) {
        Job j = new Job();
        j.setId(id);
        j.setTitle(title);
        j.setLocation("Pune");
        j.setDescriptionText("desc");
        return j;
    }

    @Test
    void putsTheDecisionOnAll() {
        Job j1 = job(1, "Java");
        Job j2 = job(2, "Account");

        when(jobFilter.evaluate("Java", "Pune", "desc")).thenReturn(FilterDecision.pass());
        when(jobFilter.evaluate("Account", "Pune", "desc")).thenReturn(FilterDecision.reject("No jobs found"));

        jobFilterService.markAll(List.of(j1, j2));

        verify(jobRepository).saveAll(List.of(j1, j2));
        assertTrue(j1.isPassedFilter());
        assertFalse(j2.isPassedFilter());
    }

    @Test
    void howManyJobsItReFilter() {

        Job j1 = job(1, "Java");
        Job j2 = job(2, "Account");
        Job j3 = job(1, "Java Developer");
        Job j4 = job(2, "Software");

        when(jobRepository.findByActiveTrueOrderByFirstSeenAtDesc()).thenReturn(
                List.of(j1, j2, j3, j4)
        );
        when(jobFilter.evaluate("Java", "Pune", "desc")).thenReturn(FilterDecision.pass());
        when(jobFilter.evaluate("Account", "Pune", "desc")).thenReturn(FilterDecision.reject("No jobs found"));
        when(jobFilter.evaluate("Java Developer", "Pune", "desc")).thenReturn(FilterDecision.pass());
        when(jobFilter.evaluate("Software", "Pune", "desc")).thenReturn(FilterDecision.pass());


        RefilterResult result = jobFilterService.reFilterAll();
        assertEquals(3, result.passed());
        assertEquals(4,result.totalChecked());
    }

}
