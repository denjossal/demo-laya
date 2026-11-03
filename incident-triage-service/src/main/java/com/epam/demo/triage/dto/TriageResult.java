package com.epam.demo.triage.dto;

public record TriageResult(
        String department,
        Double departmentConfidence,
        double urgency,
        double churnRisk,
        boolean needsHumanReview,
        String routedModel
) {
}
