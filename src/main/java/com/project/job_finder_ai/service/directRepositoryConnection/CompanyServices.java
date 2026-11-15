package com.project.job_finder_ai.service.directRepositoryConnection;

import com.project.job_finder_ai.dto.CompanyRequest;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.exception.DuplicateResourceException;
import com.project.job_finder_ai.exception.ResourceNotFoundException;
import com.project.job_finder_ai.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for company-related operations.
 *
 * <p>It contains simple business logic on top of {@link CompanyRepository}:
 * adding, finding, and activating companies.
 *
 * <ul>
 *   <li>{@link #addCompany(CompanyRequest)} – create a new company</li>
 *   <li>{@link #listAll()} – get every company</li>
 *   <li>{@link #listActive()} – get only active companies</li>
 *   <li>{@link #getById(Long)} – get one company by id</li>
 *   <li>{@link #setActive(Long)} – mark a company as active</li>
 * </ul>
 *
 * @see Company
 * @see CompanyRepository
 */
@Service
@RequiredArgsConstructor
public class CompanyServices {
    private final CompanyRepository repository;

    /**
     * Creates a new company from the given request and saves it.
     *
     * <p>The {@link CompanyRequest} is converted into a {@link Company} entity,
     * then stored using the repository.
     *
     * @param request the company details sent by the client
     * @return the saved {@link Company}
     */
    public Company addCompany(CompanyRequest request) {
        if(repository.existsByAtsTypeAndBoardSlug(request.atsType(),request.boardSlug())){
            throw new DuplicateResourceException("Duplicate company has been found!");
        }
        Company c1 = Company.builder()
                .atsType(request.atsType())
                .name(request.name())
                .boardSlug(request.boardSlug())
                .careersUrl(request.careersUrl())
                .lastFetchedAt(null)
                .build();
        return repository.save(c1);
    }

    /**
     * Returns all companies stored in the database.
     *
     * @return list of all companies (empty if none exist)
     */
    public List<Company> listAll() {
        return repository.findAll();
    }

    /**
     * Marks the company with the given id as active
     * (sets its {@code active} field to {@code true}).
     *
     * @param id the id of the company to activate
     * @return the updated {@link Company}
     * @throws ResourceNotFoundException if no company exists with this id
     */
    @Transactional
    public Company setActive(Long id) {
        Company c1 = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Enter valid Company's Id!")
        );

        c1.setActive(true);
        return c1;
    }

    @Transactional
    public Company deActive(Long id) {
        Company c1 = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Enter valid Company's Id!")
        );

        c1.setActive(false);
        return c1;
    }

    /**
     * Returns only the companies whose {@code active} field is {@code true}.
     *
     * @return list of active companies (empty if none)
     */
    public List<Company> listActive() {
        return repository.findByActiveTrue();
    }

    /**
     * Finds a company by its id.
     *
     * @param id the id of the company
     * @return the matching {@link Company}
     * @throws ResourceNotFoundException if no company exists with this id
     */
    public Company getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }

}
