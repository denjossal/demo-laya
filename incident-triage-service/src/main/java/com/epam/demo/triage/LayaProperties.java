package com.epam.demo.triage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "laya")
public record LayaProperties(String baseUrl, double confidenceThreshold) {
}
