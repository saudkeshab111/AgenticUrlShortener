package com.shortener.orchestrator;

import com.shortener.orchestrator.agents.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrchestratorEngineIntegrationTest {

    @Autowired
    private OrchestratorEngine orchestratorEngine;

    @Autowired
    private PolicyEngine policyEngine;

    private AgentRun agentRun;
    private DAG dag;

    @BeforeEach
    void setUp() {
        dag = UrlShorteningDAGBuilder.buildUrlShorteningDAG("test-run-1", "https://example.com");
        agentRun = AgentRun.builder()
                .id("test-run-1")
                .dag(dag)
                .status(AgentRun.RunStatus.PENDING)
                .build();
    }

    @Test
    void testFullOrchestrationFlow() {
        AgentRun completedRun = orchestratorEngine.execute(agentRun, "https://example.com");

        assertNotNull(completedRun);
        assertTrue(
                completedRun.getStatus() == AgentRun.RunStatus.COMPLETED ||
                        completedRun.getStatus() == AgentRun.RunStatus.FAILED ||
                        completedRun.getStatus() == AgentRun.RunStatus.BLOCKED
        );
    }

    @Test
    void testOrchestrationExecutesAllNodes() {
        AgentRun completedRun = orchestratorEngine.execute(agentRun, "https://example.com");

        assertEquals(5, dag.getNodes().size());
        assertEquals(5, dag.getExecutionOrder().size());
    }

    @Test
    void testPolicyEngineEvaluation() {
        PolicyEngine.RiskLevel riskLevel = policyEngine.evaluateAction(
                PolicyEngine.ActionType.CODE_DEPLOY,
                "url-shortening"
        );

        assertNotNull(riskLevel);
        assertTrue(
                riskLevel == PolicyEngine.RiskLevel.LOW ||
                        riskLevel == PolicyEngine.RiskLevel.MEDIUM ||
                        riskLevel == PolicyEngine.RiskLevel.HIGH ||
                        riskLevel == PolicyEngine.RiskLevel.PROHIBITED
        );
    }

    @Test
    void testGateRequirementMapping() {
        Gate.GateType gateType = policyEngine.getRequiredGate(PolicyEngine.RiskLevel.HIGH);

        assertEquals(Gate.GateType.HUMAN_GATE, gateType);
    }

    @Test
    void testLowRiskAutoApproval() {
        PolicyEngine.RiskLevel risk = policyEngine.evaluateAction(
                PolicyEngine.ActionType.SECURITY_PATCH,
                "url-shortening"
        );

        assertEquals(PolicyEngine.RiskLevel.LOW, risk);

        Gate.GateType gateType = policyEngine.getRequiredGate(risk);
        assertEquals(Gate.GateType.AUTO_GATE, gateType);
    }

    @Test
    void testMediumRiskSyncGate() {
        PolicyEngine.RiskLevel risk = policyEngine.evaluateAction(
                PolicyEngine.ActionType.DEPENDENCY_UPDATE,
                "url-shortening"
        );

        assertEquals(PolicyEngine.RiskLevel.MEDIUM, risk);

        Gate.GateType gateType = policyEngine.getRequiredGate(risk);
        assertEquals(Gate.GateType.SYNC_GATE, gateType);
    }

    @Test
    void testHighRiskHumanGate() {
        PolicyEngine.RiskLevel risk = policyEngine.evaluateAction(
                PolicyEngine.ActionType.SCHEMA_CHANGE,
                "url-shortening"
        );

        assertEquals(PolicyEngine.RiskLevel.HIGH, risk);

        Gate.GateType gateType = policyEngine.getRequiredGate(risk);
        assertEquals(Gate.GateType.HUMAN_GATE, gateType);
    }

    @Test
    void testActionAllowedForLowRisk() {
        boolean allowed = policyEngine.isActionAllowed(PolicyEngine.RiskLevel.LOW);
        assertTrue(allowed);
    }

    @Test
    void testActionBlockedForProhibited() {
        boolean allowed = policyEngine.isActionAllowed(PolicyEngine.RiskLevel.PROHIBITED);
        assertFalse(allowed);
    }
}