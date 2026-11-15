package com.project.job_finder_ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient client(ChatClient.Builder chatClient) {
        return chatClient
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
