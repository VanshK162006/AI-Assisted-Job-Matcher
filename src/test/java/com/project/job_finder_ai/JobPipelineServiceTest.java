package com.project.job_finder_ai;

import com.project.job_finder_ai.dto.NotifyResult;
import com.project.job_finder_ai.dto.ScoringResult;
import com.project.job_finder_ai.dto.summary.FetchAllResult;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.entity.FetchRun;
import com.project.job_finder_ai.enums.RunStatus;
import com.project.job_finder_ai.exception.PipelineBusyException;
import com.project.job_finder_ai.service.CompanyFetchServices;
import com.project.job_finder_ai.service.JobPipelineService;
import com.project.job_finder_ai.service.MatchingRunService;
import com.project.job_finder_ai.service.TelegramNotificationService;
import com.project.job_finder_ai.service.directRepositoryConnection.CompanyServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPipelineServiceTest {

    @Mock private CompanyServices companyService;
    @Mock private CompanyFetchServices companyFetchService;
    @Mock
    private MatchingRunService matchingRunService;
    @Mock private TelegramNotificationService notificationService;
    @InjectMocks
    private JobPipelineService pipeline;

    private Company company(String name) {
        Company c = new Company();
        c.setName(name);
        return c;
    }

    private FetchRun run(RunStatus status, int found, int created, String error) {
        FetchRun r = new FetchRun();
        r.setRunStatus(status);
        r.setJobsFound(found);
        r.setNewJobs(created);
        r.setErrorMessage(error);
        return r;
    }

    private void scoringAndTelegramAreQuiet() {
        when(matchingRunService.scorePending()).thenReturn(new ScoringResult(0, 0, 0, "OK"));
        when(notificationService.notifyNewMatches()).thenReturn(new NotifyResult(false, 0, 0, "none"));
    }

    @Test
    void addsUpTheNumbersAndListsFailures() {
        Company a = company("A"), b = company("B"), c = company("C");
        when(companyService.listActive()).thenReturn(List.of(a, b, c));
        when(companyFetchService.fetchJobsOfCompany(a)).thenReturn(run(RunStatus.SUCCESS, 10, 2, null));
        when(companyFetchService.fetchJobsOfCompany(b)).thenReturn(run(RunStatus.FAILED, 0, 0, "404"));
        when(companyFetchService.fetchJobsOfCompany(c)).thenReturn(run(RunStatus.SUCCESS, 5, 1, null));
        scoringAndTelegramAreQuiet();

        FetchAllResult r = pipeline.runAll();

        assertEquals(3, r.companiesTried());
        assertEquals(1, r.companiesFailed());
        assertEquals(15, r.jobsFound());
        assertEquals(3, r.newJobs());
        assertEquals(List.of("B: 404"), r.failures());
    }

    @Test
    void scoringOrTelegramProblemsNeverSpoilTheRun() {
        when(companyService.listActive()).thenReturn(List.of());
        when(matchingRunService.scorePending()).thenThrow(new RuntimeException("AI down"));
        when(notificationService.notifyNewMatches()).thenThrow(new RuntimeException("Telegram down"));

        FetchAllResult r = pipeline.runAll();

        assertTrue(r.scoring().note().contains("failed"));
        assertTrue(r.notification().note().contains("failed"));
    }

    @Test
    void aSecondRunWhileOneIsRunningIsRefused() {
        Company a = company("A");
        when(companyService.listActive()).thenReturn(List.of(a));
        when(companyFetchService.fetchJobsOfCompany(a)).thenAnswer(inv -> {
            // we are "inside" a run now, so starting another one must fail
            assertThrows(PipelineBusyException.class, () -> pipeline.runAll());
            return run(RunStatus.SUCCESS, 1, 0, null);
        });
        scoringAndTelegramAreQuiet();

        pipeline.runAll();

        assertDoesNotThrow(() -> pipeline.runAll());   // the lock was released afterwards
    }
}