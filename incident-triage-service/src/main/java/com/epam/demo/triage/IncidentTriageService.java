package com.epam.demo.triage;

import com.epam.demo.triage.dto.LayaPredictResponse;
import com.epam.demo.triage.dto.TriageResult;
import org.springframework.stereotype.Service;

@Service
public class IncidentTriageService {

    private final LayaClient layaClient;
    private final LayaProperties layaProperties;

    public IncidentTriageService(LayaClient layaClient, LayaProperties layaProperties) {
        this.layaClient = layaClient;
        this.layaProperties = layaProperties;
    }

    public TriageResult triage(String incidentText) {
        LayaPredictResponse response = layaClient.triage(incidentText);

        LayaPredictResponse.Answer department = response.answers().get("department");
        LayaPredictResponse.Answer urgency = response.answers().get("urgency");
        LayaPredictResponse.Answer churnRisk = response.answers().get("churn_risk");

        // Gate on answer_confidence (max p). The entropy-based `confidence` is uncalibrated, so a
        // missing answer_confidence is sent to a human rather than approximated with it.
        Double departmentConfidence = department.answerConfidence();
        boolean needsHumanReview = departmentConfidence == null
                || departmentConfidence < layaProperties.confidenceThreshold();

        return new TriageResult(
                department.choice(),
                departmentConfidence,
                urgency.score(),
                churnRisk.noul(),
                needsHumanReview,
                response.routing().model()
        );
    }
}
