package com.project.job_finder_ai.service.ai;

import com.project.job_finder_ai.dto.JobMatchView;
import com.project.job_finder_ai.dto.JobView;
import com.project.job_finder_ai.enums.ApplicationStatus;
import com.project.job_finder_ai.exception.ResourceNotFoundException;
import com.project.job_finder_ai.repository.JobMatchRepository;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.service.JobMatchService;
import com.project.job_finder_ai.service.JobServices;
import com.project.job_finder_ai.service.directRepositoryConnection.CandidateProfileService;
import com.project.job_finder_ai.service.directRepositoryConnection.CompanyServices;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AgentTools {

    private static final String LIMIT_MESSAGE =
        "Tool call limit reached. Answer the user with the information you already have.";
    private static final String INVALID_STATUS =
        "Invalid status. Use NEW, APPLIED, INTERVIEW, REJECTED or IGNORED.";

    // keep this field order: AgentService creates the object in this order
    private final JobMatchService jobMatchService;
    private final JobServices jobService;
    private final CandidateProfileService profileService;
    private final CompanyServices companyService;
    private final JobRepository jobRepository;
    private final JobMatchRepository matchRepository;
    private final AgentTrace trace;
    private final int maxResults;

    @Tool(description = "Get the candidate's profile summary: years of experience, skills, "
        + "target roles and preferred locations.")
    public String getMyProfile() {
        if (!trace.allow("getMyProfile()")) return LIMIT_MESSAGE;
        return profileService.find()
            .map(p -> "Years of experience: " + p.getYearsOfExperience()
                + "\nSkills: " + join(p.getSkills())
                + "\nTarget roles: " + join(p.getTargetRoles())
                + "\nPreferred locations: " + join(p.getPreferredLocations()))
            .orElse("No profile saved yet.");
    }

    @Tool(description = "List the candidate's job matches, best score first. "
        + "Every line starts with the match id.")
    public String searchMatches(
            @ToolParam(description = "Minimum score from 0 to 100", required = false) Integer minScore,
            @ToolParam(description = "NEW, APPLIED, INTERVIEW, REJECTED or IGNORED. "
                + "Leave empty for all statuses", required = false) String status,
            @ToolParam(description = "How many matches to return", required = false) Integer limit) {

        int min = minScore == null ? 0 : minScore;
        int max = capped(limit);
        if (!trace.allow("searchMatches(minScore=" + min + ", status=" + status + ", limit=" + max + ")")) {
            return LIMIT_MESSAGE;
        }
        ApplicationStatus parsed;
        try {
            parsed = parseStatus(status);
        } catch (IllegalArgumentException e) {
            return INVALID_STATUS;
        }
        List<JobMatchView> matches = jobMatchService.list(min, parsed, max);
        if (matches.isEmpty()) return "No matches found.";
        return matches.stream().map(this::matchLine).collect(Collectors.joining("\n"));
    }

    @Tool(description = "Get the full details of ONE match by its match id: score, reason, "
        + "matched and missing skills, status and apply link.")
    public String getMatchDetails(
            @ToolParam(description = "The match id, the number after # in a match list") Long matchId) {

        if (!trace.allow("getMatchDetails(" + matchId + ")")) return LIMIT_MESSAGE;
        if (matchId == null) return "matchId is required.";
        try {
            JobMatchView m = jobMatchService.get(matchId);
            return "Match #" + m.id()
                + "\nJob: " + oneLine(m.title(), 100) + " at " + oneLine(m.company(), 60)
                + "\nLocation: " + oneLine(m.location(), 60)
                + "\nScore: " + m.score()
                + "\nStatus: " + m.status()
                + "\nReason: " + oneLine(m.reason(), 300)
                + "\nMatched skills: " + oneLine(join(m.matchedSkills()), 150)
                + "\nMissing skills: " + oneLine(join(m.missingSkills()), 150)
                + "\nApply link: " + oneLine(m.applyUrl(), 300);
        } catch (ResourceNotFoundException e) {
            return "No match with id " + matchId;
        }
    }

    @Tool(description = "Search saved jobs by words in the job title. "
        + "Returns job ids, titles, companies and locations.")
    public String searchJobs(
            @ToolParam(description = "Comma separated words, at least one must be in the title, "
                + "for example java,backend") String keywords,
            @ToolParam(description = "true to keep only jobs in India. Default true", required = false) Boolean indiaOnly,
            @ToolParam(description = "true to keep only jobs that passed the candidate's filter rules. "
                + "Default true", required = false) Boolean onlyPassedFilter,
            @ToolParam(description = "How many jobs to return", required = false) Integer limit) {

        if (!trace.allow("searchJobs(keywords=" + keywords + ")")) return LIMIT_MESSAGE;
        List<String> words = words(keywords);
        if (words.isEmpty()) return "Please give at least one keyword.";

        boolean india = indiaOnly == null || indiaOnly;
        Boolean passed = (onlyPassedFilter == null || onlyPassedFilter) ? Boolean.TRUE : null;

        List<JobView> jobs = jobService.search(null, words, null, null, india, passed, capped(limit));
        if (jobs.isEmpty()) return "No jobs found.";
        return jobs.stream()
            .map(j -> "job #" + j.id() + " | " + oneLine(j.title(), 100)
                + " | " + oneLine(j.company(), 60)
                + " | " + oneLine(j.location(), 60)
                + " | first seen " + (j.firstSeenAt() == null ? "-" : j.firstSeenAt().toString().substring(0, 10)))
            .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Show counts: active companies, active jobs, jobs that passed the filter, "
        + "and matches per status.")
    public String getStats() {
        if (!trace.allow("getStats()")) return LIMIT_MESSAGE;
        StringBuilder sb = new StringBuilder();
        sb.append("Active companies: ").append(companyService.listActive().size()).append('\n');
        sb.append("Active jobs: ").append(jobRepository.countByActiveTrue()).append('\n');
        sb.append("Active jobs that passed the filter: ")
          .append(jobRepository.countByActiveTrueAndPassedFilterTrue());
        for (ApplicationStatus s : ApplicationStatus.values()) {
            sb.append("\nMatches ").append(s).append(": ").append(matchRepository.countByStatus(s));
        }
        return sb.toString();
    }

    @Tool(description = "Change the status of ONE match. Only use this when the user clearly asks "
        + "to change the status of a specific match.")
    public String updateMatchStatus(
            @ToolParam(description = "The match id") Long matchId,
            @ToolParam(description = "NEW, APPLIED, INTERVIEW, REJECTED or IGNORED") String status) {

        if (!trace.allow("updateMatchStatus(" + matchId + ", " + status + ")")) return LIMIT_MESSAGE;
        if (matchId == null) return "matchId is required.";
        ApplicationStatus parsed;
        try {
            parsed = parseStatus(status);
        } catch (IllegalArgumentException e) {
            return INVALID_STATUS;
        }
        if (parsed == null) return INVALID_STATUS;
        try {
            JobMatchView m = jobMatchService.updateStatus(matchId, parsed);
            return "Match #" + m.id() + " (" + oneLine(m.title(), 100) + " at "
                + oneLine(m.company(), 60) + ") is now " + parsed;
        } catch (ResourceNotFoundException e) {
            return "No match with id " + matchId;
        }
    }

    // ---------- helpers ----------
    private String matchLine(JobMatchView m) {
        return "#" + m.id() + " | score " + m.score()
            + " | " + oneLine(m.title(), 100)
            + " | " + oneLine(m.company(), 60)
            + " | " + oneLine(m.location(), 60)
            + " | " + m.status()
            + " | missing: " + oneLine(join(m.missingSkills()), 120);
    }

    private int capped(Integer limit) {
        int wanted = limit == null ? 10 : limit;
        return Math.max(1, Math.min(wanted, maxResults));
    }

    private static ApplicationStatus parseStatus(String text) {
        if (text == null || text.isBlank()) return null;
        return ApplicationStatus.valueOf(text.trim().toUpperCase());
    }

    private static List<String> words(String text) {
        if (text == null) return List.of();
        return Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .limit(10)
            .toList();
    }

    private static String join(Collection<String> items) {
        return (items == null || items.isEmpty()) ? "-" : String.join(", ", items);
    }

    // One line only, so text from a stranger cannot fake extra lines like "SYSTEM: ..."
    private static String oneLine(String text, int max) {
        if (text == null || text.isBlank()) return "-";
        String line = text.replaceAll("[\\r\\n]+", " ").trim();
        return line.length() > max ? line.substring(0, max) + "…" : line;
    }
}