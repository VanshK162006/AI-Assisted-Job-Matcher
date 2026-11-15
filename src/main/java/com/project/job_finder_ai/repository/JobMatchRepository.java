package com.project.job_finder_ai.repository;

import com.project.job_finder_ai.entity.JobMatch;
import com.project.job_finder_ai.enums.ApplicationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobMatchRepository
        extends JpaRepository<JobMatch, Long>, JpaSpecificationExecutor<JobMatch> {

    @Query("""
        select m from JobMatch m
        where m.notifiedAt is null and m.status = :status and m.score >= :minScore
        order by m.score desc, m.scoredAt desc
        """)
    List<JobMatch> findToNotify(@Param("status") ApplicationStatus status,
                                @Param("minScore") int minScore, Pageable pageable);

    @Query("""
        select count(m) from JobMatch m
        where m.notifiedAt is null and m.status = :status and m.score >= :minScore
        """)
    long countToNotify(@Param("status") ApplicationStatus status, @Param("minScore") int minScore);

    long countByStatus(ApplicationStatus status);

    @Query("""
           select m from JobMatch m
           where m.notifiedAt != null
           order by m.score desc, m.scoredAt desc
           """)
    List<JobMatch> findNotifiedJobMatch();
}