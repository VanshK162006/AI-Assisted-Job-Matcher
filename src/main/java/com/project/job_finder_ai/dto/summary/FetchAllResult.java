package com.project.job_finder_ai.dto.summary;

import com.project.job_finder_ai.dto.NotifyResult;
import com.project.job_finder_ai.dto.ScoringResult;

import java.util.List;
public record FetchAllResult(int companiesTried, int companiesFailed, int jobsFound,
                             int newJobs, List<String> failures,
                             ScoringResult scoring, NotifyResult notification) {}