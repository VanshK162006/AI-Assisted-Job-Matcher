package com.project.job_finder_ai.service;

import com.project.job_finder_ai.dto.JobMatchView;
import com.project.job_finder_ai.dto.MatchResult;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.entity.JobMatch;
import com.project.job_finder_ai.enums.ApplicationStatus;
import com.project.job_finder_ai.exception.ResourceNotFoundException;
import com.project.job_finder_ai.repository.JobMatchRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final JobMatchRepository repository;

    // skill text longer than the database column would crash the save
    private static Set<String> shorten(List<String> skills) {
        Set<String> out = new LinkedHashSet<>();
        if (skills == null) return out;
        for (String s : skills) {
            if (s != null) out.add(s.length() > 100 ? s.substring(0, 100) : s);
        }
        return out;
    }

    public JobMatch save(Job job, MatchResult result) {
        JobMatch m = new JobMatch();
        m.setJob(job);
        m.setScore(Math.toIntExact(result.score()));
        m.setReason(result.reason());
        m.setMatchedSkills(shorten(result.matchedSkills()));
        m.setMissingSkills(shorten(result.missingSkills()));
        m.setStatus(ApplicationStatus.NEW);
        m.setScoredAt(Instant.now());
        return repository.save(m);
    }

    public List<JobMatchView> list(int minScore, ApplicationStatus status, int limit) {
        Specification<JobMatch> spec =
                JobMatchSpecifications.minScore(Math.max(0, Math.min(100, minScore)));
        if (status != null) {
            spec = spec.and(JobMatchSpecifications.status(status));
        }
        Pageable page = PageRequest.of(0, Math.max(1, Math.min(limit, 200)),
                Sort.by(Sort.Direction.DESC, "score").and(Sort.by(Sort.Direction.DESC, "scoredAt")));

        return repository.findAll(spec, page).getContent().stream()
                .map(JobMatchView::from).toList();
    }

    @Transactional
    public void markNotified(Collection<Long> matchIds) {
        Instant now = Instant.now();
        for (JobMatch m : repository.findAllById(matchIds)) {
            m.setNotifiedAt(now);   // no save() needed: the transaction writes the change
        }
    }

    public JobMatchView get(Long id) {
        return JobMatchView.from(findOrThrow(id));
    }

    public JobMatchView updateStatus(Long id, ApplicationStatus status) {
        JobMatch m = findOrThrow(id);
        m.setStatus(status);
        m.setStatusUpdatedAt(Instant.now());
        return JobMatchView.from(repository.save(m));
    }

    private JobMatch findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }
}