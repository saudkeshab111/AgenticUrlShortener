package com.shortener.orchestrator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRun {

    public enum RunStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        BLOCKED  // <- ADD THIS
    }

    private String id;
    private DAG dag;

    @Builder.Default
    private RunStatus status = RunStatus.PENDING;

    @Builder.Default
    private Instant startTime = Instant.now();

    private Instant endTime;
    private String errorMessage;

    @Builder.Default
    private Map<String, Object> agentOutputs = new LinkedHashMap<>();

    /**
     * Set agent output for a node
     */
    public void setAgentOutput(String nodeId, Object output) {
        agentOutputs.put(nodeId, output);
    }

    /**
     * Get agent output for a node
     */
    public Map<String, Object> getAgentOutput(String nodeId) {
        Object output = agentOutputs.get(nodeId);
        return output instanceof Map ? (Map<String, Object>) output : new LinkedHashMap<>();
    }

    /**
     * Get all agent outputs
     */
    public Map<String, Object> getAllAgentOutputs() {
        return new LinkedHashMap<>(agentOutputs);
    }

    /**
     * Validate all nodes executed
     */
    public boolean validateAllNodesExecuted() {
        return status == RunStatus.COMPLETED && !agentOutputs.isEmpty();
    }
}