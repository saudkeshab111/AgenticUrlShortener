# Agentic URL Shortener: Architecture Document

## System Overview

This document details the architecture of the agentic SDLC orchestrator with integrated URL shortener application. It reflects the current implementation, including known gaps.

---

## Layer 1: Orchestrator Engine

### Core Components

**AgentRun (State Model)**
- Tracks execution state of a DAG run
- Maintains node execution states, decisions, and execution log
- Supports statuses: INITIALIZED, IN_PROGRESS, AWAITING_GATE, AWAITING_HUMAN, COMPLETED, FAILED, STOPPED

**DAG (Directed Acyclic Graph)**
- Represents workflow as a graph of dependent tasks
- Implements topological sort (Kahn's algorithm; in-degree = number of unmet dependencies)
- Validates no cycles exist (throws on cycle)
- Returns executable nodes at each step (pending nodes whose dependencies have all PASSED)

**DAGNode (Graph Node)**
- Represents a single orchestration task
- Has agent type, dependencies, execution state, required gate, and retry count
- States: PENDING, IN_PROGRESS, PASSED, FAILED, BLOCKED_BY_GATE, AWAITING_HUMAN, SKIPPED
- Tracks output and error messages

**UrlShorteningDAGBuilder (Workflow Builder)**
- Builds the 5-node workflow for each orchestrated request, tagged with the workflow ID
- Flow: requirement → architecture → security → [QUALITY_GATE] → implementation → [QUALITY_GATE] → test

**Gate (Control Checkpoint)**
- Types: HUMAN_GATE, AUTO_GATE, SYNC_GATE, QUALITY_GATE, SAFE_STOP
- States: PENDING, AWAITING_HUMAN_DECISION, APPROVED, REJECTED, EXPIRED
- Records approver, approval time, and reason

**PolicyEngine (Risk-Based Governance)**
- Evaluates risk level of each action
- Risk levels: LOW (auto-approve), MEDIUM (sync gate), HIGH (human approval), PROHIBITED (safe-stop)
- Action risk: SCHEMA_CHANGE = HIGH, DATABASE_MIGRATION = HIGH, CODE_DEPLOY = MEDIUM, DEPENDENCY_UPDATE = MEDIUM, SECURITY_PATCH = LOW
- Determines required gate type per risk level
- Blocks prohibited actions (`isActionAllowed`)

**GateEvaluationService (Gate Decisions)**
- Converts an operation name to an action type and asks the PolicyEngine for its risk
- LOW risk → AUTO_GATE (immediately APPROVED); any other risk → HUMAN_GATE (AWAITING_HUMAN_DECISION)
- Fail-safe: unknown or blank operations are treated as HIGH risk
- Records gate decision history; supports manual approve/reject; checks `allGatesPassed` / `anyGateBlocked`

**OrchestratorEngine (Main Executor)**
- Orchestrates DAG execution in dependency order
- Invokes agents for each node
- Applies gate validation after nodes that require it
- Logs every step tagged with the workflow ID

**UrlShorteningOrchestrator (Entry Point)**
- Creates the execution trace, builds and runs the DAG, calls ShortUrlService
- Completes or fails the trace with timing; returns status SUCCESS or FAILED

### Recovery Strategies

**RetryStrategy**
- Bounded retries (default 3 attempts)
- Exponential backoff (100ms, 200ms, 400ms), capped at 10s
- Prevents infinite loops on persistent failures

**RollbackStrategy**
- Tracks actions taken during execution
- Rolls back changes in reverse order (LIFO)
- Status: NOT_STARTED → IN_PROGRESS → COMPLETED / FAILED

**SafeStopHandler**
- Triggered on prohibited actions, critical errors, manual stop, resource exhaustion, or timeout
- Executes cleanup sequence
- Calls rollback to undo changes
- Logs stop reason and message

> Recovery components are unit-tested and exercised in DemoFlows. The live orchestrated request currently runs the happy path.

---

## Layer 2: Agent Layer

### 5 Mock Agents

All agents are **MOCK** (no Claude API calls) for demo purposes. They return fixed, structured output regardless of input.

**RequirementAgent**
- Analyzes requirements and produces structured outputs
- Outputs: normalized requirement, acceptance criteria, assumptions, ambiguities, tasks

**ArchitectureAgent**
- Proposes system design and architecture
- Outputs: layers, components, data model, patterns, risks

**SecurityAgent**
- Validates security concerns and mitigations
- Outputs: threats, mitigations, controls, compliance notes

**ImplementationAgent**
- Plans implementation phases and dependencies
- Outputs: phases, dependencies, estimated hours, risk areas

**TestAgent**
- Generates test strategy and coverage targets
- Outputs: coverage target, unit tests, integration tests, E2E tests, frameworks, test data

Agent outputs are returned in the orchestrated response under `agentOutputs`. They are not yet consumed by later stages.

---

## Layer 3: Application Layer (URL Shortener)

### API Endpoints

**POST /api/v1/urls**
- Create a shortened URL directly
- Request: `{"originalUrl": "...", "expirationDays": 30}`
- Response (201): `{"code": "AA2B3C4D", "shortUrl": "http://localhost:8080/s/AA2B3C4D", "originalUrl": "...", "createdAt": "...", "expiresAt": "..."}`
- Returns 400 for missing/invalid URL or invalid expirationDays

**GET /api/v1/{code}**
- Retrieve original URL by short code (JSON)
- Records a click (atomic increment)
- Returns 404 if unknown or expired
- Response (200): `{"code": "AA2B3C4D", "originalUrl": "...", "clickCount": 5, "createdAt": "...", "expiresAt": "..."}`

**GET /s/{code}**
- The actual short link
- Records a click and returns 302 redirect to the original URL
- Returns 404 if unknown or expired

**POST /api/v1/urls/orchestrated**
- Create a shortened URL through the full agent workflow
- Request: `{"originalUrl": "...", "expirationDays": 7}`
- Response (201): `{"workflowId": "...", "shortCode": "...", "status": "SUCCESS", "executionTimeMs": 380, "agentOutputs": {...}}`
- Returns 400 for invalid input or a failed workflow (body keeps the `workflowId`); 500 for unexpected errors (generic message)

**GET /api/v1/orchestration/traces/{workflowId}**
- Execution trace for one workflow
- Returns 404 if not found

**GET /api/v1/orchestration/traces?status=COMPLETED**
- Traces filtered by status (default COMPLETED)
- Returns 400 for an invalid status, listing allowed values

**Request Parsing Rules (both create endpoints)**
- `originalUrl`: required, non-blank string
- `expirationDays`: optional; number or numeric string; must be ≥ 0; missing or 0 → 365-day default

### Service Layer

**ShortUrlService**
- Create shortened URLs with validation (365-day default expiry when `expirationDays` ≤ 0)
- Generate unique codes: 8 uppercase hex characters, collision-checked, up to 5 retries
- Retrieve URLs; `resolveAndRecordClick` rejects expired URLs (404, click not counted)
- Record clicks atomically: `UPDATE short_urls SET click_count = click_count + 1 WHERE code = ?`
- Delete URLs

**UrlValidator**
- Validates URL format (http/https required, host required)
- SSRF protection: blocks private and internal addresses
  - localhost / 127.0.0.0/8 (loopback)
  - 10.0.0.0/8 (private)
  - 172.16.0.0/12 (private; full range coverage to be verified)
  - 192.168.0.0/16 (private)
  - 169.254.169.254 (cloud metadata endpoint)

**ExecutionTraceService**
- Creates, completes, fails, and blocks traces per workflow
- Records status, short code, original URL, error, and execution time

### Data Model

**ShortUrl Entity**
- `id` (Long, primary key)
- `code` (String, max 10 chars, currently 8, unique, not null)
- `originalUrl` (TEXT, not null)
- `createdAt` (Instant, not null; set by service, `@PrePersist` fills only if missing)
- `expiresAt` (Instant, nullable, expiration time)
- `clickCount` (Long, not null, default 0 via `@Builder.Default`)

**ExecutionTrace Entity**
- One record per workflow: workflow ID, status (e.g. COMPLETED, FAILED, BLOCKED), short code, original URL, error, execution time

### Database

- **H2 in-memory** for development/testing (`jdbc:h2:mem:testdb`)
- **auto-DDL:** create-drop (fresh database on startup)
- **Schema** created automatically from JPA entities
- **H2 console** at `/h2-console`

### API Documentation

- **Swagger UI** at `/swagger-ui/index.html`
- **OpenAPI JSON** at `/v3/api-docs`

---

## 5 Critical Demo Flows

Run via `DemoFlowsTest`. These are scripted demonstrations of the intended orchestration behavior.

### Flow 1: Requirement Decomposition (Greenfield)
- Input: "Add expiration support"
- RequirementAgent produces 7 tasks, 3 ambiguities, and assumptions
- Status: COMPLETED

### Flow 2: Parallel Orchestration + Sync Gate
- Architecture and Security nodes run in parallel
- SYNC_GATE waits for both to complete before merging
- Status: arch = PASSED, sec = PASSED, merge = PASSED

### Flow 3: Failure → Retry → Pass
- Attempt 1 fails (NPE)
- RetryStrategy allows retry 1/3 with 100ms backoff
- Attempt 2 passes

### Flow 4: High-Risk Action → Human Approval
- Action: schema change → Risk level HIGH
- HUMAN_GATE → AWAITING_HUMAN_DECISION
- Human approver (alice) approves → gate APPROVED

### Flow 5: Requirement Change → Replanning (Brownfield)
- Original DAG: 4 nodes (req → arch → impl → test)
- Change: "Add authentication support"
- Invalidated: arch, impl, test
- New DAG: 5 nodes (req → arch → sec → impl → test)
- Status: REPLANNING COMPLETE

---

## Observability

**Execution Traces**
- Persisted per workflow; queryable by ID or status

**Structured Logs**
- Tagged with workflow ID: DAG build, each node execution, each gate decision, completion/failure, duration

**Gate History**
- Recorded by GateEvaluationService

**Timing**
- `executionTimeMs` on every orchestrated response and stored on the trace

---

## Testing Strategy

**100 tests across 16 suites**

- **Graph correctness:** topological order, executable-node selection, cycle detection
- **Governance:** risk per action, risk → gate mapping, fail-safe defaults, approval/rejection workflows
- **Concurrency safety:** 1000 clicks verified exact (no lost updates); 100 concurrent creates with no collisions
- **End-to-end:** MockMvc flows covering create → persist → trace → retrieve → click recorded, plus failure traces
- **Mocked unit tests:** verify what the service actually builds (save returns its argument), not canned fixtures

---

## Known Gaps

**Agents**
- Agents are fixed mocks; output doesn't reflect input
- No context passing between stages

**Gates and Governance**
- Live quality gates always approve; they don't evaluate agent output
- No human-approval API; the live workflow never pauses for a person
- Two gate mappings: PolicyEngine maps MEDIUM → SYNC_GATE, GateEvaluationService maps MEDIUM → HUMAN_GATE
- PROHIBITED is unreachable (no action maps to it); EXPIRED gate status is unused (no gate timeouts)

**Orchestration**
- Parallel paths, retry, rollback, and replanning are shown in DemoFlows and tests, not in the live request flow
- SDLC agents run per URL request; engineering workflow and product runtime are coupled

**Application**
- SSRF checks are host-based (no DNS resolution, limited IPv6)
- No rate limiting or authentication
- No reliability metrics endpoint (success rate, MTTR, retry/rollback frequency)

---

## Next Steps

- Separate SDLC runs: `POST /api/v1/sdlc/runs` accepting a requirement (greenfield / brownfield / ambiguous)
- Input-driven agents with context passed between stages (optional Claude API with mock fallback)
- Gates that evaluate real criteria
- Human checkpoint API with run pause and resume
- Wire parallel branches, retry/rollback, and replanning into OrchestratorEngine
- Single source of truth for risk → gate mapping in PolicyEngine
- Reliability metrics derived from execution traces