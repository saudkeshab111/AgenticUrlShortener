package com.shortener.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GateIntegrationTest {

    private GateEvaluationService gateService;
    private PolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngine();
        gateService = new GateEvaluationService(policyEngine);
    }

    @Test
    void testHighRiskOperationRequiresHumanGate() {
        Gate gate = gateService.evaluateAndCreateGate("SCHEMA_CHANGE", "users table");

        assertEquals(Gate.GateType.HUMAN_GATE, gate.getType());
        // Human gates are created already waiting for a human decision
        assertEquals(Gate.GateStatus.AWAITING_HUMAN_DECISION, gate.getStatus());
    }

    @Test
    void testLowRiskOperationRequiresAutoGate() {
        Gate gate = gateService.evaluateAndCreateGate("SECURITY_PATCH", "junit");

        assertEquals(Gate.GateType.AUTO_GATE, gate.getType());
    }

    @Test
    void testGateApprovalWorkflow() {
        Gate gate = new Gate("gate-1", Gate.GateType.HUMAN_GATE, "critical-operation");

        gate.awaitHuman();
        assertEquals(Gate.GateStatus.AWAITING_HUMAN_DECISION, gate.getStatus());

        gate.approveByHuman("alice");
        assertTrue(gate.isPassed());
        assertEquals("alice", gate.getApproverName());

        gateService.recordGateDecision(gate);
        assertTrue(gateService.getGateHistory().containsKey("gate-1"));
    }

    @Test
    void testGateRejectionWorkflow() {
        Gate gate = new Gate("gate-2", Gate.GateType.QUALITY_GATE, "low-coverage");

        gate.reject("Coverage below 80%");
        assertTrue(gate.isRejected());
        assertFalse(gate.isPassed());

        gateService.recordGateDecision(gate);
    }

    @Test
    void testMultipleGatesDecision() {
        Gate gate1 = new Gate("gate-1", Gate.GateType.QUALITY_GATE, "node-1");
        Gate gate2 = new Gate("gate-2", Gate.GateType.HUMAN_GATE, "node-2");

        gate1.approve("Auto-approved");
        gate2.approveByHuman("bob");

        List<Gate> gates = List.of(gate1, gate2);
        assertTrue(gateService.allGatesPassed(gates));
        assertFalse(gateService.anyGateBlocked(gates));
    }

    @Test
    void testGateBlocksExecution() {
        Gate gate = new Gate("gate-3", Gate.GateType.SAFE_STOP, "emergency");

        gate.reject("System emergency detected");
        assertTrue(gate.isBlocked());

        List<Gate> gates = List.of(gate);
        assertFalse(gateService.allGatesPassed(gates));
        assertTrue(gateService.anyGateBlocked(gates));
    }
}