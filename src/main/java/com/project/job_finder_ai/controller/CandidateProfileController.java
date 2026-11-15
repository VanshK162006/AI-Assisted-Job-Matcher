package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.dto.candidate.ProfileRequest;
import com.project.job_finder_ai.dto.candidate.ProfileView;
import com.project.job_finder_ai.dto.summary.ResumeTextResponse;
import com.project.job_finder_ai.service.directRepositoryConnection.CandidateProfileService;
import com.project.job_finder_ai.service.util.ResumeParserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService service;
    private final ResumeParserService resumeParser;

    @PostMapping
    public ProfileView save(@Valid @RequestBody ProfileRequest request) {
        return ProfileView.from(service.save(request));
    }

    @GetMapping
    public ProfileView get() {
        return ProfileView.from(service.getOrThrow());
    }

    @PostMapping(value = "/resume/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeTextResponse extract(@RequestParam("file") MultipartFile file) throws IOException {
        String text = resumeParser.extractTextFromFile(file);
        return new ResumeTextResponse(text, text.length());
    }
}