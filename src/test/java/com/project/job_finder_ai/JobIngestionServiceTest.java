package com.project.job_finder_ai;

import com.project.job_finder_ai.dto.NormalizedJob;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.service.directRepositoryConnection.JobIngestionServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobIngestionServiceTest {
    private static final Instant LONG_AGO = Instant.parse("2020-01-01T00:00:00Z");
    private final Company company = new Company();
    @Mock
    private JobRepository jobRepository;
    @InjectMocks
    private JobIngestionServices jobIngestionServices;

    private Job saved(String externalId, boolean active) {
        Job j = new Job();
        j.setCompany(company);
        j.setExternalId(externalId);
        j.setTitle("Old");
        j.setActive(active);
        j.setLastSeenAt(LONG_AGO);
        return j;
    }

    private NormalizedJob fetched(String id, String title) {
        return new NormalizedJob(id, title, "Pune", "Engineering", "Full-time",
                "https://x.com/" + id, "desc", Instant.now());
    }

    @Test
    void newJobsSavedAndReturned() {
        when(jobRepository.findByCompanyAndExternalId(company, "101")).thenReturn(Optional.empty());
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.findByCompanyAndActiveTrue(any(Company.class))).thenReturn(List.of());

        List<Job> result = jobIngestionServices.latestJobs(company, List.of(fetched("101", "Java Developer")));
        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
        assertEquals("101", result.get(0).getExternalId());
        assertTrue(result.get(0).isActive());
        assertNotNull(result.get(0).getFirstSeenAt());
    }

    @Test
    void aKnownJobIsRefreshedButNotReturnedAsNewJob() {
        when(jobRepository.findByCompanyAndExternalId(company, "101")).thenReturn(Optional.of(saved("101", true)));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Job> jobs = jobIngestionServices.latestJobs(company, List.of(
                fetched("101", "Java Developer"),
                fetched("102", "Accounting")
        ));
        assertEquals(1, jobs.size());

    }

    @Test
    void aClosedJobThatReturnsIsReopened() {
        Job closed = saved("103", false);
        when(jobRepository.findByCompanyAndExternalId(company, "103")).thenReturn(Optional.of(closed));
        when(jobRepository.findByCompanyAndActiveTrue(company)).thenReturn(List.of());

        jobIngestionServices.latestJobs(company, List.of(fetched("103", "Java Developer")));

        assertTrue(closed.isActive());
    }
}
