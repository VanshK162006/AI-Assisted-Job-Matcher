package com.project.job_finder_ai.service.AtsFetchers;

import com.project.job_finder_ai.dto.NormalizedJob;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.enums.AtsType;

import java.util.List;

public interface JobFetcher {
    AtsType supportedType();
    List<NormalizedJob> fetch(Company company);
}