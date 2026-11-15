package com.project.job_finder_ai.repository;

import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.enums.AtsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByActiveTrue();

    boolean existsByAtsTypeAndBoardSlug(AtsType atsType, String boardSlug);
}
