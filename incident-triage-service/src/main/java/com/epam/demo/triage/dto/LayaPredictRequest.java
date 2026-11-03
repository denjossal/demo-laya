package com.epam.demo.triage.dto;

import java.util.Map;

public record LayaPredictRequest(Map<String, String> state, Map<String, Question> questions) {

    public record Question(String type, String instructions, Object criteria) {
    }
}
