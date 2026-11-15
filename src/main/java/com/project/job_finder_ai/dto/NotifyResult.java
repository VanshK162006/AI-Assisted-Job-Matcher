package com.project.job_finder_ai.dto;

public record NotifyResult(boolean sent, int matchesSent, long stillWaiting, String note) {}