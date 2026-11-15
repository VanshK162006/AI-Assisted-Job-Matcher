package com.project.job_finder_ai.dto;

import java.util.List;

public record DigestMessage(String text, List<Long> includedMatchIds) {}