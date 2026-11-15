package com.project.job_finder_ai.service.ai;

import com.project.job_finder_ai.dto.MatchResult;
import com.project.job_finder_ai.dto.PromptPreview;
import com.project.job_finder_ai.entity.CandidateProfile;
import com.project.job_finder_ai.entity.Job;
import com.project.job_finder_ai.exception.AiUnavailableException;
import com.project.job_finder_ai.exception.ResourceNotFoundException;
import com.project.job_finder_ai.repository.CandidateProfileRepository;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.service.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobMatchingServices {
    private final ChatClient chatClient;
    private final JobRepository jobRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final PromptBuilder promptBuilder;

    public MatchResult scoreJobById(Long jobId, String email) {
        Job job = findJob(jobId);
        CandidateProfile profile = candidateProfileRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Invalid Email Id!")
        );
        return score(job, profile);
    }

    public PromptPreview promptPreview(Long jobId, String email) {
        Job job = findJob(jobId);
        CandidateProfile profile = candidateProfileRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Invalid Email Id!")
        );
        String user = promptBuilder.buildUserPrompt(profile, job);
        return new PromptPreview(promptBuilder.systemPrompt(), user, user.length());
    }

    private MatchResult score(Job job, CandidateProfile profile) throws AiUnavailableException {
        Exception last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                MatchResult m1 = chatClient.prompt()
                        .system(promptBuilder.systemPrompt())
                        .user(promptBuilder.buildUserPrompt(profile, job))
                        .call()
                        .entity(MatchResult.class);
                return m1;
            } catch (Exception e) {
                last = e;
                log.warn("AI scoring attempt {} failed for job {}: {}",
                        attempt, job.getId(), e.getMessage());
            }
        }
        throw new AiUnavailableException("The AI could not score this job right now", last);
    }

    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Job not found: " + jobId)
                );
    }
}
