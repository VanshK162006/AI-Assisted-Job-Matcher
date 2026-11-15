package com.project.job_finder_ai.service.directRepositoryConnection;

import com.project.job_finder_ai.dto.FetchRunView;
import com.project.job_finder_ai.entity.FetchRun;
import com.project.job_finder_ai.repository.FetchRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FetchRunService {

    private final FetchRunRepository repository;

    public List<FetchRunView> list(Long companyId) {
        List<FetchRun> runs = companyId == null
            ? repository.findTop50ByOrderByStartedAtDesc()
            : repository.findTop50ByCompanyIdOrderByStartedAtDesc(companyId);
        return runs.stream().map(FetchRunView::from).toList();
    }

}