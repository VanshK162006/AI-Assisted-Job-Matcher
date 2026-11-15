package com.project.job_finder_ai.service.directRepositoryConnection;

import com.project.job_finder_ai.dto.candidate.ProfileRequest;
import com.project.job_finder_ai.entity.CandidateProfile;
import com.project.job_finder_ai.exception.ResourceNotFoundException;
import com.project.job_finder_ai.repository.CandidateProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository repository;

    @Transactional
    public CandidateProfile save(ProfileRequest r) {
        CandidateProfile p = repository.findFirstByOrderByIdAsc()
                .orElseGet(CandidateProfile::new);

        p.setFullName(r.fullName().trim());
        p.setEmail(r.email().trim());
        p.setYearsOfExperience(r.yearsOfExperience());
        replace(p.getSkills(), r.skills());
        replace(p.getTargetRoles(), r.targetRoles());
        replace(p.getPreferredLocations(), r.preferredLocations());
        p.setResumeText(r.resumeText().trim());
        p.setUpdatedAt(Instant.now());

        return repository.save(p);
    }

    public Optional<CandidateProfile> find() {
        return repository.findFirstByOrderByIdAsc();
    }

    public CandidateProfile getOrThrow() {
        return repository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not set up yet. Use PUT /api/profile first"));
    }

    private void replace(Set<String> target, List<String> incoming) {
        target.clear();
        if (incoming == null) return;
        incoming.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .forEach(target::add);
    }
}