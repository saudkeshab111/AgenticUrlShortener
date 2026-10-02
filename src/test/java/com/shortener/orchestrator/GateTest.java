package com.shortener.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GateTest {

    private Gate gate;

    @BeforeEach
    void setUp() {
        gate = new Gate("gate1", Gate.GateType.QUALITY_GATE, "Test Node");
    }

    @Test
    void testGateApproved() {
        gate.approve("All tests pass");

        assertTrue(gate.isPassed());
        assertEquals(Gate.GateStatus.APPROVED, gate.getStatus());
    }

    @Test
    void testGateRejected() {
        gate.reject("Coverage below 80%");

        assertTrue(gate.isRejected());
        assertEquals(Gate.GateStatus.REJECTED, gate.getStatus());
    }

    @Test
    void testGateAwaitingHuman() {
        gate.awaitHuman();

        assertTrue(gate.isAwaitingDecision());
        assertEquals(Gate.GateStatus.AWAITING_HUMAN_DECISION, gate.getStatus());
    }

    @Test
    void testHumanApproval() {
        gate.awaitHuman();
        gate.approveByHuman("alice");

        assertTrue(gate.isPassed());
        assertEquals("alice", gate.getApproverName());
    }
}