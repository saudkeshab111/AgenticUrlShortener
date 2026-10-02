# Agentic URL Shortener: Orchestration-First SDLC

## Executive Summary

This project demonstrates an **orchestration-first approach to SDLC** where:

- **Requirements flow through a DAG** (Directed Acyclic Graph)
- **5 mock agents propose outputs** (Requirement, Architecture, Security, Implementation, Test)
- **Gates validate against acceptance criteria** (4 gate types)
- **Policies govern execution** (Risk-based, human approval required for HIGH risk)
- **Tests prove criteria are met** (29 tests passing)
- **Recovery strategies handle failures** (Retry, Rollback, Safe-stop)

The **URL shortener is the application being orchestrated**, not the product. The orchestrator is the product.

---

## 🎯 Winning Pitch

> "This is an agentic SDLC orchestrator demonstrating governance-first software engineering. Requirements flow through a DAG where agents propose outputs, gates validate against acceptance criteria, and tests prove criteria are met. The orchestrator is stateful, traceable, and recoverable—supporting retry, rollback, and safe-stop. Agents operate under a risk-based policy: low-risk auto-executes, medium-risk requires validation, high-risk requires human approval, prohibited actions trigger safe-stop. The URL shortener is the application being orchestrated."

---

## 📊 Architecture Overview