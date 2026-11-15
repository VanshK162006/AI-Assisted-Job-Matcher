package com.project.job_finder_ai.repository;

import com.project.job_finder_ai.entity.FetchRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FetchRunRepository extends JpaRepository<FetchRun, Long> {
    List<FetchRun> findTop50ByOrderByStartedAtDesc();
    List<FetchRun> findTop50ByCompanyIdOrderByStartedAtDesc(Long companyId);
}