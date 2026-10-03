package com.shortener.orchestrator;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Gate: a control point in the DAG that requires approval/validation
 */
@Data
@NoArgsConstructor
public class Gate {

    public enum GateType {
        HUMAN_GATE,
        AUTO_GATE,
        SYNC_GATE,
        QUALITY_GATE,
        SAFE_STOP
    }

    public enum GateStatus {
        PENDING,
        AWAITING_HUMAN_DECISION,
        APPROVED,
        REJECTED,
        EXPIRED
    }

    private String id;
    private GateType type;
    private String nodeId;
    private GateStatus status = GateStatus.PENDING;
    private Instant createdAt = Instant.now();
    private Instant approvedAt;
    private String approvedBy;
    private String reason;

    /**
     * Constructor for test: accepts only id, type, nodeId
     */
    public Gate(String id, GateType type, String nodeId) {
        this.id = id;
        this.type = type;
        this.nodeId = nodeId;
        this.status = GateStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void awaitHuman() {
        this.status = GateStatus.AWAITING_HUMAN_DECISION;
    }

    public void approve(String reason) {
        this.status = GateStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.reason = reason;
    }

    public void approveByHuman(String approver) {
        this.status = GateStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.approvedBy = approver;
    }

    public void reject(String reason) {
        this.status = GateStatus.REJECTED;
        this.reason = reason;
    }

    public boolean isAwaitingDecision() {
        return status == GateStatus.AWAITING_HUMAN_DECISION;
    }

    public boolean isPassed() {
        return status == GateStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == GateStatus.REJECTED;
    }

    public String getApproverName() {
        return approvedBy;
    }

    public boolean isOpen() {
        return status == GateStatus.APPROVED;
    }

    public boolean isBlocked() {
        return status == GateStatus.REJECTED || status == GateStatus.PENDING;
    }
}