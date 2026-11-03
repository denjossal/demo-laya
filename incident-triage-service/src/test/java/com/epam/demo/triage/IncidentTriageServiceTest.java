package com.epam.demo.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.epam.demo.triage.dto.LayaPredictResponse;
import com.epam.demo.triage.dto.TriageResult;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentTriageServiceTest {

    @Mock
    private LayaClient layaClient;

    private final LayaProperties layaProperties = new LayaProperties("http://localhost:8000", 0.75);

    @Test
    void routesAutomaticallyWhenDepartmentConfidenceIsHigh() {
        when(layaClient.triage("billing dispute")).thenReturn(responseWithDepartmentConfidence("billing", 0.94));

        IncidentTriageService service = new IncidentTriageService(layaClient, layaProperties);
        TriageResult result = service.triage("billing dispute");

        assertThat(result.department()).isEqualTo("billing");
        assertThat(result.needsHumanReview()).isFalse();
    }

    @Test
    void flagsForHumanReviewWhenDepartmentConfidenceIsBelowThreshold() {
        when(layaClient.triage("ambiguous ticket")).thenReturn(responseWithDepartmentConfidence("billing", 0.50));

        IncidentTriageService service = new IncidentTriageService(layaClient, layaProperties);
        TriageResult result = service.triage("ambiguous ticket");

        assertThat(result.needsHumanReview()).isTrue();
    }

    @Test
    void flagsForHumanReviewWhenAnswerConfidenceIsMissing() {
        // High entropy-based confidence must not stand in for a missing answer_confidence.
        Map<String, LayaPredictResponse.Answer> answers = Map.of(
                "department", new LayaPredictResponse.Answer("billing", null, null, 0.99, null),
                "urgency", new LayaPredictResponse.Answer(null, 1.5, null, 0.9, 0.9),
                "churn_risk", new LayaPredictResponse.Answer(null, null, 0.2, null, null)
        );
        when(layaClient.triage("no answer_confidence"))
                .thenReturn(new LayaPredictResponse(answers, new LayaPredictResponse.Routing("english", "latin script")));

        TriageResult result = new IncidentTriageService(layaClient, layaProperties).triage("no answer_confidence");

        assertThat(result.needsHumanReview()).isTrue();
    }

    private static LayaPredictResponse responseWithDepartmentConfidence(String choice, double answerConfidence) {
        Map<String, LayaPredictResponse.Answer> answers = Map.of(
                "department", new LayaPredictResponse.Answer(choice, null, null, 0.3, answerConfidence),
                "urgency", new LayaPredictResponse.Answer(null, 1.5, null, 0.3, 0.9),
                "churn_risk", new LayaPredictResponse.Answer(null, null, 0.2, null, null)
        );
        return new LayaPredictResponse(answers, new LayaPredictResponse.Routing("english", "latin script"));
    }
}
