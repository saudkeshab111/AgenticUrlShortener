package com.shortener.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Policy engine for evaluating risk levels and policies
 */
@Slf4j
@Component
public class PolicyEngine {

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        PROHIBITED
    }

    public enum ActionType {
        SCHEMA_CHANGE,
        SECURITY_PATCH,
        DEPENDENCY_UPDATE,
        CODE_DEPLOY,
        DATABASE_MIGRATION
    }

    /**
     * Evaluate risk level for an action
     */
    public RiskLevel evaluateAction(ActionType action, String target) {
        log.debug("Evaluating risk for action: {} on target: {}", action, target);

        return switch (action) {
            case SCHEMA_CHANGE -> RiskLevel.HIGH;
            case DATABASE_MIGRATION -> RiskLevel.HIGH;
            case SECURITY_PATCH -> RiskLevel.LOW;
            case CODE_DEPLOY -> RiskLevel.MEDIUM;
            case DEPENDENCY_UPDATE -> RiskLevel.MEDIUM;
        };
    }

    /**
     * Get required gate based on risk level
     */
    public Gate.GateType getRequiredGate(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case PROHIBITED -> Gate.GateType.SAFE_STOP;
            case HIGH -> Gate.GateType.HUMAN_GATE;
            case MEDIUM -> Gate.GateType.SYNC_GATE;
            case LOW -> Gate.GateType.AUTO_GATE;
        };
    }

    /**
     * Check if action is allowed based on risk level
     */
    public boolean isActionAllowed(RiskLevel riskLevel) {
        return riskLevel != RiskLevel.PROHIBITED;
    }

    /**
     * Get risk description
     */
    public String getRiskDescription(RiskLevel level) {
        return switch (level) {
            case LOW -> "Low risk - auto-approved";
            case MEDIUM -> "Medium risk - requires sync gate";
            case HIGH -> "High risk - requires human approval";
            case PROHIBITED -> "Prohibited - operation blocked";
        };
    }
}