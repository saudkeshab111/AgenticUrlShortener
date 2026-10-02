package com.shortener.orchestrator;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validation gates that control execution flow.
 * Each gate checks acceptance criteria before allowing a node to proceed.
 */
@Data
@NoArgsConstructor
public class Gate {

    private String id;
    private GateType type;
    private String nodeName;
    private GateStatus status = GateStatus.PENDING;
    private String validationMessage;
    private String approverName; // For HUMAN_GATE

    public enum GateType {
        REQUIRE_GATE,        // Validates requirements against acceptance criteria
        ARCHITECTURE_GATE,   // Validates design against architecture
        HUMAN_GATE,          // Requires human approval
        QUALITY_GATE,        // Validates test coverage and quality metrics
        RELEASE_GATE         // Validates readiness for deployment
    }

    public enum GateStatus {
        PENDING,
        APPROVED,
        REJECTED,
        AWAITING_HUMAN_DECISION
    }

    public Gate(String id, GateType type, String nodeName) {
        this.id = id;
        this.type = type;
        this.nodeName = nodeName;
    }

    public void approve(String validationMessage) {
        this.status = GateStatus.APPROVED;
        this.validationMessage = validationMessage;
    }

    public void reject(String validationMessage) {
        this.status = GateStatus.REJECTED;
        this.validationMessage = validationMessage;
    }

    public void awaitHuman() {
        this.status = GateStatus.AWAITING_HUMAN_DECISION;
    }

    public void approveByHuman(String approverName) {
        this.status = GateStatus.APPROVED;
        this.approverName = approverName;
    }

    public boolean isPassed() {
        return this.status == GateStatus.APPROVED;
    }

    public boolean isRejected() {
        return this.status == GateStatus.REJECTED;
    }

    public boolean isAwaitingDecision() {
        return this.status == GateStatus.AWAITING_HUMAN_DECISION;
    }
}