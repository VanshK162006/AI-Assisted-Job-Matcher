package com.project.job_finder_ai.service.ai;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class AgentTrace {
    private final int maxCalls;
    private final List<String> calls = new ArrayList<>();

    public synchronized boolean allow(String description) {
        if (calls.size() >= maxCalls) return false;
        calls.add(description);
        return true;
    }

    public synchronized List<String> calls() {
        return List.copyOf(calls);
    }
}
