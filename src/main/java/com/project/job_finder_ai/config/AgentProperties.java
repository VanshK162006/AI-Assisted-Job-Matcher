package com.project.job_finder_ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent")
@Getter
@Setter
public class AgentProperties {
    private boolean enabled = true;
    private String model;
    private int maxToolCalls = 8;
    private int maxMessageChars = 500;
    private int maxResultsPerTool = 15;
    private int maxRememberedTurns = 6;
}