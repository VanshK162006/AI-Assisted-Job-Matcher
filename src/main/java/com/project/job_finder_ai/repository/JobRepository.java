package com.project.job_finder_ai.repository;

import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.enums.ApplicationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    Optional<Job> findByCompanyAndExternalId(Company company, String externalId);

    List<Job> findByCompanyAndActiveTrue(Company company);

    List<Job> findByActiveTrueOrderByFirstSeenAtDesc();

    List<Job> findByPassedFilterTrueAndActiveTrueOrderByPostedAtDesc(Pageable pageable);

    @Query("""
        select j from Job j
        where j.active = true
          and j.passedFilter = true
          and not exists (select m.id from JobMatch m where m.job = j)
        order by j.firstSeenAt desc
        """)
    List<Job> findPendingForScoringAndPassedFilterTrue(Pageable pageable);


    @Query("""
        select count(m) from Job m
        where m.notifiedAt is null and m.passedFilter = true
        """)
    long countToNotify();

    long countByActiveTrue();
    long countByActiveTrueAndPassedFilterTrue();
}