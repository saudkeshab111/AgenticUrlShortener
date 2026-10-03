package com.shortener.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class GateEvaluationService {

    private final PolicyEngine policyEngine;
    private final Map<String, Gate> gateHistory = new LinkedHashMap<>();

    public GateEvaluationService(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    /**
     * Evaluate risk and determine required gate.
     * LOW risk          → AUTO_GATE (auto-approved)
     * Any other risk    → HUMAN_GATE (awaiting decision)
     * Unknown operation → treated as HIGH risk (fail safe)
     */
    public Gate evaluateAndCreateGate(String operation, String target) {
        PolicyEngine.RiskLevel riskLevel = resolveRiskLevel(operation, target);

        Gate.GateType gateType = mapRiskLevelToGateType(riskLevel);
        Gate.GateStatus initialStatus = getInitialStatusForGateType(gateType);

        Gate gate = new Gate(
                "gate-" + UUID.randomUUID().toString().substring(0, 8),
                gateType,
                target
        );
        gate.setStatus(initialStatus);

        log.info("Created gate {} for operation {} with risk level {} → gate type {} (status: {})",
                gate.getId(), operation, riskLevel, gateType, initialStatus);

        return gate;
    }

    /**
     * Convert the operation name to an ActionType and ask the PolicyEngine for its risk.
     * Unknown or blank operations are treated as HIGH risk so they always need human review.
     */
    private PolicyEngine.RiskLevel resolveRiskLevel(String operation, String target) {
        if (operation == null || operation.isBlank()) {
            log.warn("No operation provided; treating as HIGH risk");
            return PolicyEngine.RiskLevel.HIGH;
        }

        try {
            PolicyEngine.ActionType actionType =
                    PolicyEngine.ActionType.valueOf(operation.trim().toUpperCase());
            return policyEngine.evaluateAction(actionType, target);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown operation '{}'; treating as HIGH risk", operation);
            return PolicyEngine.RiskLevel.HIGH;
        }
    }

    /**
     * Map RiskLevel to GateType.
     * Only LOW risk is auto-approved; everything else (including null/unknown) requires a human.
     */
    private Gate.GateType mapRiskLevelToGateType(PolicyEngine.RiskLevel riskLevel) {
        if (riskLevel == PolicyEngine.RiskLevel.LOW) {
            return Gate.GateType.AUTO_GATE;
        }
        return Gate.GateType.HUMAN_GATE;
    }

    /**
     * AUTO_GATE  → immediately APPROVED
     * HUMAN_GATE → AWAITING_HUMAN_DECISION
     */
    private Gate.GateStatus getInitialStatusForGateType(Gate.GateType gateType) {
        if (gateType == Gate.GateType.AUTO_GATE) {
            return Gate.GateStatus.APPROVED;
        }
        return Gate.GateStatus.AWAITING_HUMAN_DECISION;
    }

    /**
     * Create a gate with explicit approval (for testing or manual override)
     */
    public Gate createGateWithDecision(String operation, String target, Gate.GateType gateType, Gate.GateStatus status) {
        Gate gate = new Gate(
                "gate-" + UUID.randomUUID().toString().substring(0, 8),
                gateType,
                target
        );
        gate.setStatus(status);

        log.info("Created gate {} for operation {} with explicit type {} and status {}",
                gate.getId(), operation, gateType, status);

        return gate;
    }

    /**
     * Record gate decision
     */
    public void recordGateDecision(Gate gate) {
        gateHistory.put(gate.getId(), gate);
        log.info("Recorded gate decision: {} → {}", gate.getId(), gate.getStatus());
    }

    /**
     * Get gate history
     */
    public Map<String, Gate> getGateHistory() {
        return new LinkedHashMap<>(gateHistory);
    }

    /**
     * Get gate by ID
     */
    public Gate getGateById(String gateId) {
        return gateHistory.get(gateId);
    }

    /**
     * Check if all required gates are passed
     */
    public boolean allGatesPassed(List<Gate> gates) {
        return gates.stream().allMatch(Gate::isPassed);
    }

    /**
     * Check if any gate is blocked
     */
    public boolean anyGateBlocked(List<Gate> gates) {
        return gates.stream().anyMatch(Gate::isBlocked);
    }

    /**
     * Approve a gate (for manual approval workflows)
     */
    public void approveGate(String gateId, String approverName) {
        Gate gate = gateHistory.get(gateId);
        if (gate != null && gate.getStatus() == Gate.GateStatus.AWAITING_HUMAN_DECISION) {
            gate.setStatus(Gate.GateStatus.APPROVED);
            gate.setApprovedBy(approverName);
            log.info("Gate {} approved by {}", gateId, approverName);
        }
    }

    /**
     * Reject a gate (for manual rejection workflows)
     */
    public void rejectGate(String gateId, String rejectorName) {
        Gate gate = gateHistory.get(gateId);
        if (gate != null && gate.getStatus() == Gate.GateStatus.AWAITING_HUMAN_DECISION) {
            gate.setStatus(Gate.GateStatus.REJECTED);
            gate.setApprovedBy(rejectorName);
            log.info("Gate {} rejected by {}", gateId, rejectorName);
        }
    }
}