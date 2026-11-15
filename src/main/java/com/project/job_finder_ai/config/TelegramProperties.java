package com.project.job_finder_ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram")
@Getter
@Setter
public class TelegramProperties {
    private boolean enabled = false;
    private String botToken;
    private String chatId;
    private String apiBaseUrl = "https://api.telegram.org";
    @Value("${telegram.min-score}")
    private int minScore;
    @Value("${telegram.max-jobs-per-message:10}")
    private int maxJobsPerMessage;
}