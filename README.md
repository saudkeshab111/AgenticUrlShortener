# Agentic URL Shortener: Orchestration-First SDLC

## System Overview

This project demonstrates an orchestration-first approach to SDLC. The orchestrator is the product; the URL shortener is the application being orchestrated.

---

## Executive Summary

**Orchestration**
- Requirements flow through a DAG (Directed Acyclic Graph) with topological ordering and cycle detection
- 5 mock agents propose outputs: Requirement, Architecture, Security, Implementation, Test
- Quality gates are checked between stages

**Governance**
- Risk-based PolicyEngine: LOW, MEDIUM, HIGH, PROHIBITED
- Gate types: AUTO_GATE, HUMAN_GATE, SYNC_GATE, QUALITY_GATE, SAFE_STOP
- Fail-safe: unknown actions are treated as HIGH risk; only LOW risk auto-approves

**Recovery**
- Retry with exponential backoff (3 attempts: 100ms, 200ms, 400ms)
- Rollback in reverse order (LIFO)
- Safe-stop on prohibited actions or critical errors

**Traceability**
- Every orchestrated run is persisted as an execution trace
- Traces are queryable by workflow ID or status

**Quality**
- 100 tests passing across 16 suites

---

## Winning Pitch

> "This is an agentic SDLC orchestrator demonstrating governance-first software engineering. Requirements flow through a DAG where agents propose outputs, gates check them, and a risk-based policy decides what proceeds automatically, what needs a human, and what is blocked. The orchestrator is stateful, traceable, and recoverable, supporting retry, rollback, and safe-stop. The URL shortener is the application being orchestrated."

---

## Quick Start

**Prerequisites**
- JDK 17
- Maven 3.8+ (or IntelliJ's bundled Maven)
- Git

**Run Tests**
- Command: `mvn clean test`
- Expected: `Tests run: 100, Failures: 0` → `BUILD SUCCESS`

**Start the Application**
- Command: `mvn spring-boot:run`
- Or in IntelliJ: open `UrlShortenerApplication.java` → click the green ▶
- Expected: `Started UrlShortenerApplication`

**Tools**
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:testdb`, user `sa`, blank password)
- Note: the database is in-memory; all data is lost when the app stops

---

## Example Run (Swagger UI)

**Step 1: Create via the orchestrator**
- Endpoint: POST `/api/v1/urls/orchestrated`
- Request: `{"originalUrl": "https://github.com", "expirationDays": 7}`
- Expected: 201, `status: SUCCESS`, `workflowId`, `shortCode`, and `agentOutputs` from all 5 agents

**Step 2: Use the short link**
- Open `http://localhost:8080/s/<shortCode>` in a browser
- Expected: redirects to GitHub and counts a click

**Step 3: Check clicks**
- Endpoint: GET `/api/v1/{code}`
- Expected: 200 with `clickCount`

**Step 4: View the audit trail**
- Endpoint: GET `/api/v1/orchestration/traces/{workflowId}`
- Expected: 200 with the execution trace

**Step 5: Guardrail checks (orchestrated POST)**
- `{"originalUrl": "not-a-url"}` → 400, invalid URL, trace recorded as FAILED
- `{"originalUrl": "http://192.168.1.1/admin"}` → 400, private network blocked (SSRF)
- `{"originalUrl": ""}` → 400, originalUrl is required
- `{"originalUrl": "https://x.com", "expirationDays": -1}` → 400, negative expiration rejected

**Watching it run**
- The IntelliJ Run panel shows each step: DAG built → each agent executed → QUALITY_GATE APPROVED → DAG execution COMPLETED → trace saved

---

## API Endpoints

**POST /api/v1/urls**
- Create a short URL directly
- Responses: 201, 400

**GET /api/v1/{code}**
- JSON lookup by short code; counts a click
- Responses: 200, 404 (unknown or expired), 400

**GET /s/{code}**
- The short link; 302 redirect to the original URL; counts a click
- Responses: 302, 404

**POST /api/v1/urls/orchestrated**
- Create a short URL through the full agent workflow
- Responses: 201, 400 (invalid input or failed workflow), 500

**GET /api/v1/orchestration/traces/{workflowId}**
- Execution trace for one workflow
- Responses: 200, 404

**GET /api/v1/orchestration/traces?status=COMPLETED**
- Traces filtered by status (default COMPLETED)
- Responses: 200, 400 (invalid status)

**Request Rules**
- `originalUrl`: required, non-blank
- `expirationDays`: optional; missing or 0 = 365-day default; negative or non-numeric = 400

See ARCHITECTURE.md for full details.

---

## Project Structure

**orchestrator/** (agentic layer)
- OrchestratorEngine: runs the DAG and applies gates
- UrlShorteningOrchestrator: entry point for orchestrated requests
- UrlShorteningDAGBuilder: builds the 5-node workflow graph
- DAG / DAGNode: graph model, topological sort, cycle detection
- Gate / GateEvaluationService / PolicyEngine: governance
- AgentRun / AgentType: run state and agent types
- DemoFlows: scripted scenario demos
- agents/: Requirement, Architecture, Security, Implementation, Test agents
- recovery/: RetryStrategy, RollbackStrategy, SafeStopHandler

**urlshortener/** (application being orchestrated)
- api/: ShortUrlController, OrchestratorUrlShorteningController, GlobalExceptionHandler
- dto/: request and response DTOs
- model/: ShortUrl, ExecutionTrace
- repository/: Spring Data repositories
- service/: ShortUrlService, UrlValidator, ExecutionTraceService

---

## Testing

**Orchestrator Unit Tests**
- DAGTest (7), GateTest (4), PolicyEngineTest (4), RetryStrategyTest (3), AgentTests (5), OrchestratorEngineTest (1)

**Orchestrator Integration Tests**
- GateIntegrationTest (6), OrchestratorEngineIntegrationTest (9), DemoFlowsTest (5)

**Service Tests**
- ShortUrlServiceTest (14), ValidationTest (11), ExecutionTraceTest (8)

**API / End-to-End Tests**
- UrlShorteningEndToEndTest (6), OrchestrationPersistenceTest (4), ComprehensiveValidationTest (10)

**Performance Tests**
- PerformanceTest (3): 100 creates, 100 concurrent creates, 1000 clicks with no lost updates

---

## Key Engineering Decisions

**Fail-Safe Governance**
- Unknown operations are treated as HIGH risk; only LOW risk auto-approves

**Atomic Click Counting**
- A single `UPDATE … click_count + 1` prevents lost clicks under concurrency

**Error Handling**
- Failed workflows return 400 with the `workflowId`, so the failure trace can be inspected
- Expired URLs return 404 and are not counted as clicks
- 500 responses use a generic message; details are logged server-side only

**Defaults**
- 365-day default expiry to prevent unbounded storage growth

**Mock Agents**
- Deterministic output for reproducible demos and tests, with no external API dependency

---

## Known Limitations

**Agents**
- Agents are deterministic mocks; output doesn't reflect input and no LLM is called
- Agent outputs don't feed later stages or affect the result

**Gates**
- Quality gates in the live flow always approve
- No human-approval endpoint; gates can reach AWAITING_HUMAN_DECISION but can't be approved via API

**Orchestration**
- Parallel paths, retry, and replanning are shown in DemoFlows and tests, not in the live request flow
- SDLC agents run per URL request; engineering workflow and product runtime are coupled

**Application**
- SSRF checks are host-based (no DNS resolution, limited IPv6)
- No rate limiting or authentication
- In-memory storage only

---

## Roadmap

- Separate SDLC runs: `POST /api/v1/sdlc/runs` accepting a requirement (greenfield / brownfield / ambiguous)
- Input-driven agents with context passed between stages (optional Claude API with mock fallback)
- Gates that evaluate real criteria
- Human checkpoint API with run pause and resume
- Live parallel branches, retry/rollback in the engine, replanning on requirement change
- Reliability metrics: success rate, retry/rollback frequency, MTTR, end-to-end latency

---

## RUN TESTING
```aiignore
# Test commands
.\e2e-test.ps1 create "https://google.com" 30

.\e2e-test.ps1 get "ABC123"

.\e2e-test.ps1 orchestrate "https://github.com" 30

.\e2e-test.ps1 e2etestclear

```