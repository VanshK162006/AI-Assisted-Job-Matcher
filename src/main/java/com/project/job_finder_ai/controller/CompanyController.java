package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.dto.CompanyRequest;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.service.directRepositoryConnection.CompanyServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyServices service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Company add(@Valid @RequestBody CompanyRequest request) {
        return service.addCompany(request);
    }

    @GetMapping
    public List<Company> list() {
        return service.listAll();
    }

    @PatchMapping("/{id}/active")
    public Company setActive(@PathVariable Long id) {
        return service.setActive(id);
    }

    @PatchMapping("/{id}/de-active")
    public Company DeActive(@PathVariable Long id) {
        return service.deActive(id);
    }
}