package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.dto.AgentReply;
import com.project.job_finder_ai.dto.MatchResult;
import com.project.job_finder_ai.dto.PromptPreview;
import com.project.job_finder_ai.service.ai.AgentService;
import com.project.job_finder_ai.service.ai.JobMatchingServices;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class OpenAIController {

    private final JobMatchingServices service;
    private final AgentService agentService;

    @GetMapping("/preview/{jobId}")
    public PromptPreview preview(@RequestParam Long jobId,@RequestParam String email) {
        return service.promptPreview(jobId,email);
    }

    @PostMapping("/test/{jobId}")
    public MatchResult test(@RequestParam Long jobId,@RequestParam String email) {
        return service.scoreJobById(jobId,email);
    }

    @PostMapping
    public AgentReply chat(
            @RequestParam String message
    ){
        return agentService.chat(null,message);
    }
}