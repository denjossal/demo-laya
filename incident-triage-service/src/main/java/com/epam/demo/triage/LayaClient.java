package com.epam.demo.triage;

import com.epam.demo.triage.dto.LayaPredictRequest;
import com.epam.demo.triage.dto.LayaPredictResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LayaClient {

    private static final Map<String, LayaPredictRequest.Question> TRIAGE_QUESTIONS = Map.of(
            "department", new LayaPredictRequest.Question(
                    "choice",
                    "Which department should handle this incident?",
                    Map.of(
                            "billing", "invoices, payments, refunds",
                            "technical", "bugs, outages, system errors",
                            "other", "everything else"
                    )
            ),
            "urgency", new LayaPredictRequest.Question(
                    "score",
                    "How urgent is this incident?",
                    List.of("not urgent", "soon", "blocking")
            ),
            "churn_risk", new LayaPredictRequest.Question(
                    "noul",
                    "Does the customer threaten to cancel or leave?",
                    null
            )
    );

    private final RestClient restClient;

    public LayaClient(RestClient.Builder restClientBuilder, LayaProperties layaProperties) {
        this.restClient = restClientBuilder.baseUrl(layaProperties.baseUrl()).build();
    }

    public LayaPredictResponse triage(String incidentText) {
        LayaPredictRequest request = new LayaPredictRequest(Map.of("body", incidentText), TRIAGE_QUESTIONS);

        return restClient.post()
                .uri("/v1/systemone")
                .body(request)
                .retrieve()
                .body(LayaPredictResponse.class);
    }
}
