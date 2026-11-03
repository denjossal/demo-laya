package com.epam.demo.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.epam.demo.triage.dto.LayaPredictResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Pins the wire format against laya-serve 0.4.12. The response body below is trimmed from a real
 * /v1/systemone response captured from the sidecar.
 */
class LayaClientContractTest {

    private static final String LAYA_RESPONSE = """
            {
              "model": "english",
              "answers": {
                "department": {"type": "choice", "choice": "billing", "confidence": 0.61,
                               "answer_confidence": 0.93,
                               "probabilities": {"billing": 0.93, "technical": 0.05, "other": 0.02}},
                "urgency": {"type": "score", "score": 1.7, "confidence": 0.5, "answer_confidence": 0.8,
                            "probabilities": {"0": 0.1, "1": 0.3, "2": 0.6},
                            "legend": {"0": "not urgent", "1": "soon", "2": "blocking"}},
                "churn_risk": {"type": "noul", "noul": 0.88, "answer_confidence": 0.88}
              },
              "routing": {"model": "english", "repo": "convaiinnovations/laya",
                          "reason": "latin script", "detection": {}, "workflow": null},
              "usage": {"input_tokens": 45, "output_tokens": 0}
            }
            """;

    @Test
    void sendsSystemOneRequestAndParsesAnswerConfidence() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LayaClient client = new LayaClient(builder, new LayaProperties("http://laya:8000", 0.75));

        server.expect(requestTo("http://laya:8000/v1/systemone"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.state.body").value("billed twice"))
                .andExpect(jsonPath("$.questions.department.type").value("choice"))
                .andExpect(jsonPath("$.questions.urgency.type").value("score"))
                .andExpect(jsonPath("$.questions.churn_risk.type").value("noul"))
                .andRespond(withSuccess(LAYA_RESPONSE, MediaType.APPLICATION_JSON));

        LayaPredictResponse response = client.triage("billed twice");

        server.verify();
        LayaPredictResponse.Answer department = response.answers().get("department");
        assertThat(department.choice()).isEqualTo("billing");
        assertThat(department.answerConfidence()).isEqualTo(0.93);
        assertThat(department.confidence()).isEqualTo(0.61);
        assertThat(response.answers().get("urgency").score()).isEqualTo(1.7);
        assertThat(response.answers().get("churn_risk").noul()).isEqualTo(0.88);
        assertThat(response.routing().model()).isEqualTo("english");
    }
}
