package com.project.job_finder_ai;

import com.project.job_finder_ai.dto.NormalizedJob;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.entity.FetchRun;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.enums.AtsType;
import com.project.job_finder_ai.enums.RunStatus;
import com.project.job_finder_ai.repository.CompanyRepository;
import com.project.job_finder_ai.repository.FetchRunRepository;
import com.project.job_finder_ai.service.AtsFetchers.FetcherRegistry;
import com.project.job_finder_ai.service.AtsFetchers.JobFetcher;
import com.project.job_finder_ai.service.CompanyFetchServices;
import com.project.job_finder_ai.service.directRepositoryConnection.JobFilterService;
import com.project.job_finder_ai.service.directRepositoryConnection.JobIngestionServices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class CompanyFetchServiceTest {

    @Mock
    private FetcherRegistry fetcherRegistry;
    @Mock
    private JobFetcher fetcher;
    @Mock
    private JobIngestionServices ingestionService;
    @Mock
    private FetchRunRepository fetchRunRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private JobFilterService jobFilterService;

    @InjectMocks
    private CompanyFetchServices companyFetchServices;

    private Company company() {
        Company c = new Company();
        c.setName("Razorpay");
        c.setAtsType(AtsType.GREENHOUSE);
        return c;
    }

    private NormalizedJob njob(String id) {
        return new NormalizedJob(id, "t", "l", "d", "e", "u", "desc", null);
    }

    @BeforeEach
    void saveReturnsWhatItGets() {
        when(fetchRunRepository.save(any(FetchRun.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulFetchIsRecorded() {
        Company c = company();
        when(fetcherRegistry.getFetcher(AtsType.GREENHOUSE)).thenReturn(fetcher);
        when(fetcher.fetch(any())).thenReturn(List.of(njob("1"), njob("2"), njob("3")));
        when(ingestionService.latestJobs(any(Company.class), anyList())).thenReturn(List.of(new Job()));

        FetchRun run = companyFetchServices.fetchJobsOfCompany(c);

        assertEquals(RunStatus.SUCCESS, run.getRunStatus());
        assertEquals(3, run.getJobsFound());
        assertEquals(1, run.getNewJobs());
        assertNotNull(c.getLastFetchedAt());
        assertNotNull(run.getFinishedAt());
        verify(jobFilterService).markAll(anyList());
    }

    @Test
    void aFailureIsRecordedAndNeverThrown() {
        Company c = company();
        when(fetcherRegistry.getFetcher(AtsType.GREENHOUSE)).thenReturn(fetcher);
        when(fetcher.fetch(c)).thenThrow(new RuntimeException("404 Not Found"));

        FetchRun run = companyFetchServices.fetchJobsOfCompany(c);          // must NOT throw

        assertEquals(RunStatus.FAILED, run.getRunStatus());
        assertTrue(run.getErrorMessage().contains("404 Not Found"));
        assertNull(c.getLastFetchedAt());
        verifyNoInteractions(ingestionService);
    }
}
