package com.shortener.orchestrator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for demo flows.
 * Verifies that all 5 orchestration flows execute conceptually.
 */
class DemoFlowsTest {

    @Test
    void testFlow1RequirementDecomposition() {
        assertDoesNotThrow(() -> DemoFlows.flow1_RequirementDecomposition());
    }

    @Test
    void testFlow2ParallelOrchestration() {
        assertDoesNotThrow(() -> DemoFlows.flow2_ParallelOrchestration());
    }

    @Test
    void testFlow3FailureRetryPass() {
        assertDoesNotThrow(() -> DemoFlows.flow3_FailureRetryPass());
    }

    @Test
    void testFlow4HighRiskHumanApproval() {
        assertDoesNotThrow(() -> DemoFlows.flow4_HighRiskHumanApproval());
    }

    @Test
    void testFlow5RequirementChangeReplanning() {
        assertDoesNotThrow(() -> DemoFlows.flow5_RequirementChangeReplanning());
    }
}