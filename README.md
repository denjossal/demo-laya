# demo-laya — Spring Boot + laya incident triage

A minimal Java example showing how to call [laya](https://github.com/NandhaKishorM/laya)
— a non-autoregressive NLP decision engine — from a Spring Boot service over HTTP.

laya is not Java. It runs as a Python `laya-serve` sidecar (FastAPI, pinned to
`laya==0.4.12`). The Java side calls it via `RestClient` and applies one business
rule on top: if laya's confidence in the department routing falls below a threshold,
the ticket is flagged for human review instead of being auto-routed.

## Architecture

```
POST /incidents
      │
      ▼
[Spring Boot :8080]             incident-triage-service/
  IncidentController
      │
      ▼
  IncidentTriageService         confidence gate on answer_confidence
      │
      ▼
  LayaClient
      │  POST /v1/systemone
      ▼
[laya-serve :8000]              laya-sidecar/
  Python / FastAPI
```

## Quick start

```bash
docker compose up --build
```

First boot downloads laya's model checkpoints (~1.5 GB). Subsequent starts are fast.
Both services must be healthy before sending requests:

```bash
docker compose ps   # laya should show (healthy)
```

## Try it

**Billing dispute with churn threat**
```bash
curl -s localhost:8080/incidents \
  -H 'content-type: application/json' \
  -d '{"text": "We were billed twice for March. Refund the duplicate today or we will cancel our plan."}' \
  | python3 -m json.tool
```
Expected: `department: billing`, `churnRisk` near 0.9, `needsHumanReview: false`.

**Outage report**
```bash
curl -s localhost:8080/incidents \
  -H 'content-type: application/json' \
  -d '{"text": "The application crashes every time I open settings."}' \
  | python3 -m json.tool
```
Expected: `department: technical`.

**Ambiguous text — triggers the confidence gate**
```bash
curl -s localhost:8080/incidents \
  -H 'content-type: application/json' \
  -d '{"text": "hey question about my account"}' \
  | python3 -m json.tool
```
Expected: `needsHumanReview: true`. laya's `answer_confidence` falls below the
`laya.confidence-threshold` (default `0.75` in `application.yml`).

**Multilingual — no code changes needed**
```bash
curl -s localhost:8080/incidents \
  -H 'content-type: application/json' \
  -d '{"text": "Nos cobraron dos veces en marzo, por favor devuelvan el importe duplicado."}' \
  | python3 -m json.tool
```
laya auto-detects the language and routes to its multilingual checkpoint.
`routedModel: multilingual` will appear in the response.

**Long ticket — observe the 512-token truncation limit**
```bash
curl -s localhost:8000/v1/systemone \
  -H 'content-type: application/json' \
  -d '{"state":{"body":"<PASTE 150+ WORD TICKET HERE>"},"questions":{"department":{"type":"choice","instructions":"Which department?","criteria":{"billing":"invoices","technical":"bugs","other":"other"}}}}' \
  | python3 -m json.tool
```
Check `usage.truncated` in the raw laya response. Text beyond 512 tokens is
silently dropped; laya returns `not_stated` 93% of the time on truncated input.
Use `max_len` and `head_max_len` request fields to control the window.

## Project layout

```
demo-laya/
├── docker-compose.yml
├── laya-sidecar/
│   └── Dockerfile              # pip install "laya[serve]==0.4.12" + laya-serve
└── incident-triage-service/
    ├── Dockerfile               # Maven build + amazoncorretto:21-alpine
    ├── pom.xml                  # Spring Boot 3.3 / Java 21
    └── src/
        └── main/
            ├── java/com/epam/demo/triage/
            │   ├── LayaClient.java             # builds /v1/systemone request
            │   ├── IncidentTriageService.java  # confidence-gate rule
            │   ├── IncidentController.java     # POST /incidents
            │   ├── LayaProperties.java         # @ConfigurationProperties
            │   └── dto/
            │       ├── Incident.java
            │       ├── LayaPredictRequest.java
            │       ├── LayaPredictResponse.java  # maps answer_confidence
            │       └── TriageResult.java
            └── resources/
                └── application.yml             # laya.base-url, confidence-threshold
```

## Key implementation details

### Why `answer_confidence`, not `confidence`

laya returns two confidence fields per answer:

| Field | What it is | Use for gating? |
|---|---|---|
| `confidence` | Normalized entropy (uncalibrated) | No |
| `answer_confidence` | Max probability — `max(p)` | Yes |

The gate in `IncidentTriageService` uses `answer_confidence`. A missing field
sends the ticket to human review rather than silently falling back to the
uncalibrated value.

### Why the sidecar pattern

The upstream repo has a `laya-java/` module (an in-process ONNX port), but it
is unpublished (`0.1.0-SNAPSHOT`) and its HTTP client is not implemented.
Running `laya-serve` as a sidecar is the practical path until that ships.

### Latency expectations

| Setup | Latency |
|---|---|
| T4 GPU (laya docs) | ~33 ms per question |
| Apple M3 Pro local (independent benchmark) | ~42 ms per question |
| 4-CPU Docker VM, this demo, 3 questions | ~430 ms end to end |
| Hosted Jev API (same `/v1/systemone` protocol) | 135–410 ms depending on network |

Laya's latency grows linearly with question count (each is a separate forward
pass). The crossover with hosted Jev is at 3–4 questions per request.

### Confidence threshold

The `0.75` default in `application.yml` is a starting point. For production,
fit a threshold per question on a held-out labeled slice of your own tickets.
An independent benchmark found laya's calibration error varies from 0.110 to
0.308 per question type, so a single global value is not sufficient.

## Configuration

| Property | Default | Description |
|---|---|---|
| `laya.base-url` | `http://localhost:8000` | laya-serve base URL (overridden by `LAYA_BASE_URL` env var) |
| `laya.confidence-threshold` | `0.75` | Minimum `answer_confidence` for auto-routing the department |

### laya-serve environment variables (set in `docker-compose.yml`)

| Variable | Value | Description |
|---|---|---|
| `LAYA_HOST` | `0.0.0.0` | Bind address |
| `LAYA_PORT` | `8000` | Listen port |
| `LAYA_DEVICE` | `cpu` | Inference device (`cpu`, `cuda`, `mps`) |
| `LAYA_PRELOAD` | `1` | Load all checkpoints at startup (~6 GiB on this setup) |

## Running tests

```bash
cd incident-triage-service
mvn test   # requires Java 21
```

Four tests:
- Gate routes automatically when `answer_confidence` is high
- Gate flags for human review when `answer_confidence` is low
- Gate flags for human review when `answer_confidence` is missing entirely
- HTTP contract test against a response captured from the real laya-serve

## Requirements

- Docker and Docker Compose
- Java 21 and Maven (for running tests locally without Docker)

## References

- [laya on GitHub](https://github.com/NandhaKishorM/laya)
- [laya API docs](https://laya-ai.com/laya-api)
- [laya-serve self-hosting guide](https://laya-ai.com/guides/self-host-laya)
- [Independent Jev vs laya benchmark](https://github.com/harrymunro/jev-laya-benchmark)
