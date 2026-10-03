package com.shortener.orchestrator;

import com.shortener.orchestrator.AgentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DAGNode {

    public enum ExecutionState {
        PENDING,
        IN_PROGRESS,
        PASSED,
        FAILED,
        SKIPPED
    }

    private String id;
    private String name;
    private AgentType agentType;
    private String dependsOn;

    @Builder.Default
    private ExecutionState executionState = ExecutionState.PENDING;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private String output;
    private String errorMessage;
    private Instant completedAt;
    private long executionTimeMs;

    /**
     * Mark node as passed with output
     */
    public void markPassed(String output) {
        this.executionState = ExecutionState.PASSED;
        this.output = output;
        this.completedAt = Instant.now();
        if (createdAt != null) {
            this.executionTimeMs = completedAt.toEpochMilli() - createdAt.toEpochMilli();
        }
    }

    /**
     * Mark node as failed with error
     */
    public void markFailed(String errorMessage) {
        this.executionState = ExecutionState.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        if (createdAt != null) {
            this.executionTimeMs = completedAt.toEpochMilli() - createdAt.toEpochMilli();
        }
    }

    /**
     * Check if node can execute (dependencies met)
     */
    public boolean canExecute() {
        return executionState == ExecutionState.PENDING;
    }

    /**
     * Check if node is blocked
     */
    public boolean isBlocked() {
        return executionState == ExecutionState.FAILED;
    }
}