package com.project.job_finder_ai.dto;

import java.util.List;

public record AgentReply(String conversationId, String reply, List<String> toolsUsed) {}
