package com.shortener.orchestrator;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enforces risk-based policies on orchestrator actions.
 * Determines whether actions can auto-execute, require validation, or are prohibited.
 */
@Data
@NoArgsConstructor
public class PolicyEngine {

    public enum RiskLevel {
        LOW,           // Auto-execute (no gate needed)
        MEDIUM,        // Requires validation gate
        HIGH,          // Requires human approval
        CRITICAL,      // Escalate to security review
        PROHIBITED     // Trigger safe-stop
    }

    public enum ActionType {
        SCHEMA_CHANGE,
        DEPENDENCY_UPGRADE,
        SECURITY_PATCH,
        REQUIREMENT_CHANGE,
        ABORT_RUN
    }

    /**
     * Evaluate the risk level of an action.
     */
    public RiskLevel evaluateAction(ActionType action, String context) {
        return switch (action) {
            case SCHEMA_CHANGE -> RiskLevel.HIGH;           // Requires human approval
            case DEPENDENCY_UPGRADE -> RiskLevel.MEDIUM;    // Requires validation
            case SECURITY_PATCH -> RiskLevel.LOW;           // Auto-execute
            case REQUIREMENT_CHANGE -> RiskLevel.MEDIUM;    // Requires validation
            case ABORT_RUN -> RiskLevel.CRITICAL;           // Escalate to security
        };
    }

    /**
     * Determine gate type based on risk level.
     */
    public Gate.GateType getRequiredGate(RiskLevel risk) {
        return switch (risk) {
            case LOW -> Gate.GateType.REQUIRE_GATE;
            case MEDIUM -> Gate.GateType.QUALITY_GATE;
            case HIGH -> Gate.GateType.HUMAN_GATE;
            case CRITICAL -> Gate.GateType.RELEASE_GATE;
            case PROHIBITED -> null; // Trigger safe-stop instead
        };
    }

    /**
     * Check if action is allowed to proceed.
     */
    public boolean isActionAllowed(RiskLevel risk) {
        return risk != RiskLevel.PROHIBITED;
    }
}