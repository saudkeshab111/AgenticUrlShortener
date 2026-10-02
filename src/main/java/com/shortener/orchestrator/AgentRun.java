package com.shortener.orchestrator;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

/**
 * Represents the state of an orchestrator run.
 * Tracks the DAG execution, decisions, and metrics.
 */
@Data
@NoArgsConstructor
public class AgentRun {

    private String runId;
    private DAG dag;
    private Map<String, DAGNode.ExecutionState> nodeStates = new HashMap<>();
    private Map<String, Object> decisions = new HashMap<>();
    private List<String> executionLog = new ArrayList<>();
    private Instant startTime;
    private Instant endTime;
    private RunStatus status = RunStatus.INITIALIZED;
    private String failureReason;

    public enum RunStatus {
        INITIALIZED,
        IN_PROGRESS,
        AWAITING_GATE,
        AWAITING_HUMAN,
        COMPLETED,
        FAILED,
        STOPPED
    }

    public AgentRun(String runId, DAG dag) {
        this.runId = runId;
        this.dag = dag;
        this.startTime = Instant.now();

        // Initialize node states
        for (DAGNode node : dag.getNodes()) {
            this.nodeStates.put(node.getId(), DAGNode.ExecutionState.PENDING);
        }
    }

    public void logExecution(String message) {
        this.executionLog.add(Instant.now() + ": " + message);
    }

    public void setNodeState(String nodeId, DAGNode.ExecutionState state) {
        this.nodeStates.put(nodeId, state);
    }

    public DAGNode.ExecutionState getNodeState(String nodeId) {
        return this.nodeStates.getOrDefault(nodeId, DAGNode.ExecutionState.PENDING);
    }

    public void recordDecision(String nodeId, Object decision) {
        this.decisions.put(nodeId, decision);
    }

    public Object getDecision(String nodeId) {
        return this.decisions.get(nodeId);
    }

    public void complete() {
        this.endTime = Instant.now();
        this.status = RunStatus.COMPLETED;
    }

    public void fail(String reason) {
        this.endTime = Instant.now();
        this.status = RunStatus.FAILED;
        this.failureReason = reason;
    }

    public void stop() {
        this.endTime = Instant.now();
        this.status = RunStatus.STOPPED;
    }
}