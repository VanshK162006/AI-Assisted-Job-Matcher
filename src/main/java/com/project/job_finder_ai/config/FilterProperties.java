package com.project.job_finder_ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "filter")
@Getter
@Setter
public class FilterProperties {
    private int maxYears = 3;
    private List<String> includeTitleWords = new ArrayList<>();
    private List<String> excludeTitleWords = new ArrayList<>();
    private List<String> indiaLocationWords = new ArrayList<>();
}