package com.shortener.orchestrator;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Represents a node in the orchestration DAG (Directed Acyclic Graph).
 * Each node is a task that can depend on other nodes.
 */
@Data
@NoArgsConstructor
public class DAGNode {

    private String id;
    private String name;
    private String agentType; // REQUIREMENT, ARCHITECTURE, SECURITY, IMPLEMENTATION, TEST
    private List<String> dependsOn = new ArrayList<>();
    private ExecutionState state = ExecutionState.PENDING;
    private String output;
    private String errorMessage;
    private Gate.GateType requiredGate;
    private int retryCount = 0;

    public enum ExecutionState {
        PENDING,
        IN_PROGRESS,
        PASSED,
        FAILED,
        BLOCKED_BY_GATE,
        AWAITING_HUMAN,
        SKIPPED
    }

    public DAGNode(String id, String name, String agentType) {
        this.id = id;
        this.name = name;
        this.agentType = agentType;
    }

    public DAGNode(String id, String name, String agentType, Gate.GateType requiredGate) {
        this(id, name, agentType);
        this.requiredGate = requiredGate;
    }

    public void addDependency(String nodeId) {
        if (!this.dependsOn.contains(nodeId)) {
            this.dependsOn.add(nodeId);
        }
    }

    public boolean canExecute(Map<String, ExecutionState> allNodeStates) {
        for (String depId : this.dependsOn) {
            ExecutionState depState = allNodeStates.get(depId);
            if (depState == null || depState != ExecutionState.PASSED) {
                return false;
            }
        }
        return true;
    }

    public void markPassed(String output) {
        this.state = ExecutionState.PASSED;
        this.output = output;
    }

    public void markFailed(String error) {
        this.state = ExecutionState.FAILED;
        this.errorMessage = error;
    }

    public void markInProgress() {
        this.state = ExecutionState.IN_PROGRESS;
    }

    public void markBlockedByGate() {
        this.state = ExecutionState.BLOCKED_BY_GATE;
    }

    public void markAwaitingHuman() {
        this.state = ExecutionState.AWAITING_HUMAN;
    }
}