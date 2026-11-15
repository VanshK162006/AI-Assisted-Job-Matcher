package com.project.job_finder_ai.dto;

public record FilterDecision(boolean passed, String reason) {
    public static FilterDecision pass() { return new FilterDecision(true, "OK"); }
    public static FilterDecision reject(String reason) { return new FilterDecision(false, reason); }
}