package com.shortener.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class OrchestratorEngine {

    private final PolicyEngine policyEngine;

    public OrchestratorEngine(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    /**
     * Execute DAG with gate integration
     */
    public AgentRun execute(AgentRun run, String originalUrl) {
        log.info("Starting DAG execution for workflow: {}", run.getId());

        DAG dag = run.getDag();
        List<DAGNode> executionOrder = dag.getExecutionOrder();

        for (DAGNode node : executionOrder) {
            log.info("[{}] Executing node: {}", run.getId(), node.getId());

            try {
                // Execute agent
                Map<String, Object> output = executeAgent(node, originalUrl);
                node.markPassed(output.toString());
                run.setAgentOutput(node.getId(), output);

                log.debug("[{}] Node {} passed", run.getId(), node.getId());

                // Check for gate after this node
                Gate gate = findGateForNode(dag, node.getId());
                if (gate != null) {
                    log.info("[{}] Gate {} required after node {}", run.getId(), gate.getType(), node.getId());

                    boolean gateApproved = evaluateGate(gate, node);
                    if (!gateApproved) {
                        log.warn("[{}] Gate {} REJECTED. Halting execution.", run.getId(), gate.getType());
                        node.markFailed("Gate rejected: " + gate.getType());
                        run.setStatus(AgentRun.RunStatus.BLOCKED);
                        return run;
                    }

                    log.info("[{}] Gate {} APPROVED", run.getId(), gate.getType());
                }

            } catch (Exception e) {
                log.error("[{}] Node {} FAILED: {}", run.getId(), node.getId(), e.getMessage());
                node.markFailed(e.getMessage());
                run.setStatus(AgentRun.RunStatus.FAILED);
                return run;
            }
        }

        run.setStatus(AgentRun.RunStatus.COMPLETED);
        log.info("[{}] DAG execution COMPLETED", run.getId());
        return run;
    }

    /**
     * Execute an agent based on its type
     */
    private Map<String, Object> executeAgent(DAGNode node, String originalUrl) {
        switch (node.getAgentType()) {
            case REQUIREMENT:
                return executeRequirementAgent(node, originalUrl);
            case ARCHITECTURE:
                return executeArchitectureAgent(node, originalUrl);
            case SECURITY:
                return executeSecurityAgent(node, originalUrl);
            case IMPLEMENTATION:
                return executeImplementationAgent(node, originalUrl);
            case TEST:
                return executeTestAgent(node, originalUrl);
            default:
                throw new IllegalArgumentException("Unknown agent type: " + node.getAgentType());
        }
    }

    private Map<String, Object> executeRequirementAgent(DAGNode node, String originalUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent", "RequirementAgent");
        result.put("url", originalUrl);
        result.put("requirements", List.of("shortening", "tracking", "expiration"));
        result.put("status", "ANALYZED");
        return result;
    }

    private Map<String, Object> executeArchitectureAgent(DAGNode node, String originalUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent", "ArchitectureAgent");
        result.put("design", "REST API with distributed cache");
        result.put("components", List.of("Controller", "Service", "Repository"));
        result.put("status", "DESIGNED");
        return result;
    }

    private Map<String, Object> executeSecurityAgent(DAGNode node, String originalUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent", "SecurityAgent");
        result.put("threats", List.of("SQL injection", "XSS", "CSRF"));
        result.put("mitigations", List.of("parameterized queries", "input validation", "CSRF tokens"));
        result.put("status", "SECURED");
        return result;
    }

    private Map<String, Object> executeImplementationAgent(DAGNode node, String originalUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent", "ImplementationAgent");
        result.put("framework", "Spring Boot 3.2");
        result.put("database", "H2");
        result.put("status", "IMPLEMENTED");
        return result;
    }

    private Map<String, Object> executeTestAgent(DAGNode node, String originalUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent", "TestAgent");
        result.put("coverage", "85%");
        result.put("tests", List.of("unit", "integration", "e2e"));
        result.put("status", "TESTED");
        return result;
    }

    /**
     * Find gate that comes after a specific node
     */
    private Gate findGateForNode(DAG dag, String nodeId) {
        // Simple implementation: check if node is high-risk, add QUALITY_GATE
        if (nodeId.contains("security") || nodeId.contains("implementation")) {
            Gate gate = new Gate(
                    "gate-" + nodeId,
                    Gate.GateType.QUALITY_GATE,
                    nodeId
            );
            gate.awaitHuman();
            return gate;
        }
        return null;
    }

    /**
     * Evaluate if a gate should pass
     */
    private boolean evaluateGate(Gate gate, DAGNode node) {
        log.debug("Evaluating gate {} for node {}", gate.getType(), node.getId());

        switch (gate.getType()) {
            case AUTO_GATE:
                // Auto-gates always pass
                gate.approve("Auto-approved");
                return true;

            case QUALITY_GATE:
                // Quality gates check implementation quality
                if (node.getOutput() != null && !node.getOutput().isEmpty()) {
                    gate.approve("Quality check passed");
                    return true;
                }
                gate.reject("Quality check failed: empty output");
                return false;

            case HUMAN_GATE:
                // Human gates require approval (simulated here)
                gate.awaitHuman();
                gate.approveByHuman("system-admin");
                return gate.isPassed();

            case SYNC_GATE:
                // Sync gates are for parallel execution coordination
                gate.approve("Sync complete");
                return true;

            case SAFE_STOP:
                // Safe stop gates halt execution
                gate.reject("Safe stop engaged");
                return false;

            default:
                return true;
        }
    }
}