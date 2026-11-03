package com.epam.demo.triage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LayaPredictResponse(Map<String, Answer> answers, Routing routing) {

    /**
     * {@code confidence} is normalized entropy (uncalibrated); {@code answerConfidence} is max(p),
     * the value laya's own {@code min_confidence} gate is defined against.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Answer(
            String choice,
            Double score,
            Double noul,
            Double confidence,
            @JsonProperty("answer_confidence") Double answerConfidence) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Routing(String model, String reason) {
    }
}
