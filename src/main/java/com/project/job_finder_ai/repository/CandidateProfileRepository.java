package com.project.job_finder_ai.repository;

import com.project.job_finder_ai.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    Optional<CandidateProfile> findFirstByOrderByIdAsc();
    Optional<CandidateProfile> findByEmail(String email);

}