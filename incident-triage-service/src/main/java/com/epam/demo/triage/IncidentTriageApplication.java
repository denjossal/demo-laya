package com.epam.demo.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LayaProperties.class)
public class IncidentTriageApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentTriageApplication.class, args);
    }
}
