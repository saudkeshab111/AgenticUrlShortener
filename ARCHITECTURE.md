# Agentic URL Shortener: Architecture Document

## System Overview

This document details the architecture of the agentic SDLC orchestrator with integrated URL shortener application.

---

## Layer 1: Orchestrator Engine

### Core Components

**AgentRun (State Model)**
- Tracks execution state of a DAG run
- Maintains node execution states, decisions, and execution log
- Supports statuses: INITIALIZED, IN_PROGRESS, AWAITING_GATE, AWAITING_HUMAN, COMPLETED, FAILED, STOPPED

**DAG (Directed Acyclic Graph)**
- Represents workflow as a graph of dependent tasks
- Implements topological sort for execution order
- Validates no cycles exist
- Returns executable nodes at each step

**DAGNode (Graph Node)**
- Represents a single orchestration task
- Has agent type, dependencies, execution state
- Supports gates for validation
- Tracks output and error messages

**Gate (Validation Checkpoint)**
- Types: REQUIRE_GATE, ARCHITECTURE_GATE, HUMAN_GATE, QUALITY_GATE, RELEASE_GATE
- States: PENDING, APPROVED, REJECTED, AWAITING_HUMAN_DECISION
- Enforces acceptance criteria before proceeding

**PolicyEngine (Risk-Based Governance)**
- Evaluates risk level of each action
- Risk levels: LOW (auto-execute), MEDIUM (validation), HIGH (human), CRITICAL (escalate), PROHIBITED (stop)
- Determines required gate type per action
- Blocks prohibited actions

**OrchestratorEngine (Main Executor)**
- Orchestrates DAG execution
- Invokes agents for each node
- Applies gate validation
- Handles recovery on failures

### Recovery Strategies

**RetryStrategy**
- Bounded retries (default 3 attempts)
- Exponential backoff (100ms, 200ms, 400ms)
- Prevents infinite loops on persistent failures

**RollbackStrategy**
- Tracks actions taken during execution
- Rolls back changes in reverse order (LIFO)
- Preserves data integrity on failure

**SafeStopHandler**
- Triggered on prohibited actions or critical errors
- Executes cleanup sequence
- Calls rollback to undo changes
- Logs stop reason and message

---

## Layer 2: Agent Layer

### 5 Mock Agents

All agents are **MOCK** (no Claude API calls) for demo purposes.

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
- Outputs: unit tests, integration tests, E2E tests, test data

---

## Layer 3: Application Layer (URL Shortener)

### API Endpoints

**POST /api/v1/urls**
- Create a shortened URL
- Request: `{"originalUrl": "...", "expirationDays": 30}`
- Response: `{"code": "abc123", "shortUrl": "http://localhost:8080/abc123", ...}`

**GET /api/v1/{code}**
- Retrieve original URL by short code
- Records click count
- Returns 404 if expired
- Response: `{"code": "abc123", "originalUrl": "...", "clickCount": 5, ...}`

### Service Layer

**ShortUrlService**
- Create shortened URLs with validation
- Retrieve URLs with expiration checking
- Delete URLs
- Record click statistics

**UrlValidator**
- Validates URL format (http/https required)
- SSRF protection: blocks private networks
    - 127.0.0.0/8 (localhost)
    - 10.0.0.0/8 (private)
    - 172.16.0.0/12 (private)
    - 192.168.0.0/16 (private)
    - 169.254.169.254 (AWS metadata)
- Generates unique short codes (6 alphanumeric characters)

### Data Model

**ShortUrl Entity**
- `id` (Long, primary key)
- `code` (String, 6 char, unique, indexed)
- `originalUrl` (TEXT, not null)
- `createdAt` (Instant, creation timestamp)
- `expiresAt` (Instant, nullable, expiration time)
- `clickCount` (Long, click statistics)

### Database

- **H2 in-memory** for development/testing
- **auto-DDL:** create-drop (fresh database on startup)
- **Schema** created automatically from JPA entities

---

## 5 Critical Demo Flows

### Flow 1: Requirement Decomposition