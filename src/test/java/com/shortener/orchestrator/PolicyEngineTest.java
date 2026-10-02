package com.shortener.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolicyEngineTest {

    private PolicyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PolicyEngine();
    }

    @Test
    void testSchemaChangeIsHigh() {
        PolicyEngine.RiskLevel risk = engine.evaluateAction(
                PolicyEngine.ActionType.SCHEMA_CHANGE, "users table"
        );

        assertEquals(PolicyEngine.RiskLevel.HIGH, risk);
    }

    @Test
    void testSecurityPatchIsLow() {
        PolicyEngine.RiskLevel risk = engine.evaluateAction(
                PolicyEngine.ActionType.SECURITY_PATCH, "junit"
        );

        assertEquals(PolicyEngine.RiskLevel.LOW, risk);
    }

    @Test
    void testHighRiskRequiresHumanGate() {
        Gate.GateType gate = engine.getRequiredGate(PolicyEngine.RiskLevel.HIGH);

        assertEquals(Gate.GateType.HUMAN_GATE, gate);
    }

    @Test
    void testProhibitedActionNotAllowed() {
        boolean allowed = engine.isActionAllowed(PolicyEngine.RiskLevel.PROHIBITED);

        assertFalse(allowed);
    }
}