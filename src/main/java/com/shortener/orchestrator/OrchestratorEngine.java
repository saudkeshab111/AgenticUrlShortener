package com.shortener.orchestrator;

import com.shortener.orchestrator.agents.*;
import com.shortener.orchestrator.recovery.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * Main orchestrator engine that executes DAG-based SDLC workflows.
 * Coordinates agents, gates, and recovery strategies.
 */
@Component
@RequiredArgsConstructor
public class OrchestratorEngine {

    private final PolicyEngine policyEngine;

    /**
     * Execute a DAG run with orchestration logic.
     */
    public AgentRun execute(DAG dag, String runId) {
        AgentRun run = new AgentRun(runId, dag);
        run.setStatus(AgentRun.RunStatus.IN_PROGRESS);
        run.logExecution("Starting orchestration run: " + runId);

        try {
            // Get topological order
            List<DAGNode> executionOrder = dag.getTopologicalOrder();
            run.logExecution("Topological order: " + executionOrder.size() + " nodes");

            // Execute nodes in order
            for (DAGNode node : executionOrder) {
                executeNode(run, node);

                // Check if we should stop
                if (run.getStatus() == AgentRun.RunStatus.STOPPED) {
                    break;
                }
            }

            // Mark as complete if not already failed
            if (run.getStatus() == AgentRun.RunStatus.IN_PROGRESS) {
                run.complete();
                run.logExecution("Orchestration completed successfully");
            }

        } catch (Exception e) {
            run.fail("Orchestration failed: " + e.getMessage());
            run.logExecution("ERROR: " + e.getMessage());
        }

        return run;
    }

    /**
     * Execute a single node with gate validation.
     */
    private void executeNode(AgentRun run, DAGNode node) {
        run.setNodeState(node.getId(), DAGNode.ExecutionState.IN_PROGRESS);
        run.logExecution("Executing node: " + node.getId() + " (" + node.getName() + ")");

        try {
            // Get agent output (mock)
            String output = invokeAgent(node);
            run.recordDecision(node.getId(), output);

            // Check for gate
            if (node.getRequiredGate() != null) {
                Gate gate = new Gate(node.getId() + "-gate", node.getRequiredGate(), node.getName());

                // Evaluate policy
                PolicyEngine.RiskLevel risk = evaluateNodeRisk(node);

                if (risk == PolicyEngine.RiskLevel.PROHIBITED) {
                    // Trigger safe stop
                    SafeStopHandler handler = new SafeStopHandler(
                            SafeStopHandler.StopReason.PROHIBITED_ACTION,
                            "Prohibited action in node: " + node.getId()
                    );
                    handler.stop(new RollbackStrategy());
                    run.stop();
                    run.logExecution("SAFE_STOP triggered for node: " + node.getId());
                    return;
                } else if (risk == PolicyEngine.RiskLevel.HIGH) {
                    // Require human approval
                    gate.awaitHuman();
                    gate.approveByHuman("system-auto");
                    run.logExecution("Human gate approved for node: " + node.getId());
                } else {
                    // Auto-approve lower risk
                    gate.approve("Automated validation passed");
                    run.logExecution("Gate passed for node: " + node.getId());
                }
            }

            // Mark as passed
            node.markPassed(output);
            run.setNodeState(node.getId(), DAGNode.ExecutionState.PASSED);
            run.logExecution("Node PASSED: " + node.getId());

        } catch (Exception e) {
            node.markFailed(e.getMessage());
            run.setNodeState(node.getId(), DAGNode.ExecutionState.FAILED);
            run.logExecution("Node FAILED: " + node.getId() + " - " + e.getMessage());
        }
    }

    /**
     * Invoke the appropriate agent for a node (MOCK).
     */
    private String invokeAgent(DAGNode node) {
        return switch (node.getAgentType()) {
            case "REQUIREMENT" -> {
                RequirementAgent agent = new RequirementAgent();
                RequirementAgent.RequirementOutput output = agent.analyze("Node: " + node.getName());
                yield "Tasks: " + output.getTasks().size() + ", Criteria: " + output.getAcceptanceCriteria().size();
            }
            case "ARCHITECTURE" -> {
                ArchitectureAgent agent = new ArchitectureAgent();
                ArchitectureAgent.ArchitectureOutput output = agent.propose("Node: " + node.getName());
                yield "Components: " + output.getComponents().size() + ", Risks: " + output.getRisks().size();
            }
            case "SECURITY" -> {
                SecurityAgent agent = new SecurityAgent();
                SecurityAgent.SecurityOutput output = agent.validate("Node: " + node.getName());
                yield "Threats: " + output.getThreats().size() + ", Mitigations: " + output.getMitigations().size();
            }
            case "IMPLEMENTATION" -> {
                ImplementationAgent agent = new ImplementationAgent();
                ImplementationAgent.ImplementationOutput output = agent.plan("Node: " + node.getName());
                yield "Phases: " + output.getPhases().size() + ", Hours: " + output.getEstimatedHours();
            }
            case "TEST" -> {
                TestAgent agent = new TestAgent();
                TestAgent.TestOutput output = agent.generateStrategy("Node: " + node.getName());
                yield "Coverage: " + output.getCoverageTarget() + "%, Tests: "
                        + (output.getUnitTests().size() + output.getIntegrationTests().size() + output.getEndToEndTests().size());
            }
            default -> "Unknown agent: " + node.getAgentType();
        };
    }

    /**
     * Evaluate risk level for a node (MOCK policy).
     */
    private PolicyEngine.RiskLevel evaluateNodeRisk(DAGNode node) {
        // Mock risk evaluation based on node name
        String name = node.getName().toLowerCase();

        if (name.contains("schema") || name.contains("database")) {
            return PolicyEngine.RiskLevel.HIGH;
        } else if (name.contains("security") || name.contains("auth")) {
            return PolicyEngine.RiskLevel.HIGH;
        } else if (name.contains("dependency") || name.contains("upgrade")) {
            return PolicyEngine.RiskLevel.MEDIUM;
        } else if (name.contains("test") || name.contains("validation")) {
            return PolicyEngine.RiskLevel.LOW;
        }

        return PolicyEngine.RiskLevel.MEDIUM;
    }
}