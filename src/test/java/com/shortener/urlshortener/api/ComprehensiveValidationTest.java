package com.shortener.urlshortener.api;

import com.shortener.orchestrator.*;
import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.repository.ShortUrlRepository;
import com.shortener.urlshortener.repository.ExecutionTraceRepository;
import com.shortener.urlshortener.model.ExecutionTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ComprehensiveValidationTest {

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ExecutionTraceRepository traceRepository;

    @Autowired
    private OrchestratorEngine orchestratorEngine;

    @Autowired
    private PolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        shortUrlRepository.deleteAll();
        traceRepository.deleteAll();
    }

    @Test
    void testSystemIntegrationAllComponents() {
        assertNotNull(shortUrlRepository);
        assertNotNull(traceRepository);
        assertNotNull(orchestratorEngine);
        assertNotNull(policyEngine);
    }

    @Test
    void testDAGConstruction() {
        DAG dag = UrlShorteningDAGBuilder.buildUrlShorteningDAG("test-1", "https://example.com");

        assertNotNull(dag);
        assertEquals(5, dag.getNodes().size());
        assertTrue(dag.isValid());

        List<DAGNode> order = dag.getExecutionOrder();
        assertEquals(5, order.size());

        assertEquals("requirement-agent", order.get(0).getId());
        assertEquals("architecture-agent", order.get(1).getId());
        assertEquals("security-agent", order.get(2).getId());
        assertEquals("implementation-agent", order.get(3).getId());
        assertEquals("test-agent", order.get(4).getId());
    }

    @Test
    void testDAGNodeDependencies() {
        DAG dag = UrlShorteningDAGBuilder.buildUrlShorteningDAG("test-2", "https://example.com");

        DAGNode requirement = dag.getNodeById("requirement-agent");
        DAGNode architecture = dag.getNodeById("architecture-agent");
        DAGNode security = dag.getNodeById("security-agent");

        assertNotNull(requirement);
        assertNotNull(architecture);
        assertNotNull(security);

        assertNull(requirement.getDependsOn());
        assertEquals("requirement-agent", architecture.getDependsOn());
        assertEquals("architecture-agent", security.getDependsOn());
    }

    @Test
    void testPolicyEngineAllRiskLevels() {
        PolicyEngine.RiskLevel[] expectedRisks = {
                PolicyEngine.RiskLevel.LOW,
                PolicyEngine.RiskLevel.MEDIUM,
                PolicyEngine.RiskLevel.HIGH,
                PolicyEngine.RiskLevel.PROHIBITED
        };

        for (PolicyEngine.RiskLevel expected : expectedRisks) {
            assertNotNull(expected);
        }
    }

    @Test
    void testPolicyEngineAllActionTypes() {
        PolicyEngine.ActionType[] actions = {
                PolicyEngine.ActionType.SCHEMA_CHANGE,
                PolicyEngine.ActionType.SECURITY_PATCH,
                PolicyEngine.ActionType.DEPENDENCY_UPDATE,
                PolicyEngine.ActionType.CODE_DEPLOY,
                PolicyEngine.ActionType.DATABASE_MIGRATION
        };

        for (PolicyEngine.ActionType action : actions) {
            assertNotNull(action);
            PolicyEngine.RiskLevel risk = policyEngine.evaluateAction(action, "test");
            assertNotNull(risk);
        }
    }

    @Test
    void testGateTypesCovered() {
        Gate.GateType[] types = Gate.GateType.values();

        assertNotNull(types);
        assertTrue(types.length > 0);

        boolean hasHumanGate = false;
        boolean hasAutoGate = false;
        boolean hasSyncGate = false;

        for (Gate.GateType type : types) {
            if (type == Gate.GateType.HUMAN_GATE) hasHumanGate = true;
            if (type == Gate.GateType.AUTO_GATE) hasAutoGate = true;
            if (type == Gate.GateType.SYNC_GATE) hasSyncGate = true;
        }

        assertTrue(hasHumanGate && hasAutoGate && hasSyncGate);
    }

    @Test
    void testExecutionTraceStatusTransitions() {
        ExecutionTrace trace = ExecutionTrace.builder()
                .workflowId("test-transition")
                .originalUrl("https://example.com")
                .status(ExecutionTrace.ExecutionStatus.PENDING)
                .build();

        assertEquals(ExecutionTrace.ExecutionStatus.PENDING, trace.getStatus());

        trace.setStatus(ExecutionTrace.ExecutionStatus.RUNNING);
        assertEquals(ExecutionTrace.ExecutionStatus.RUNNING, trace.getStatus());

        trace.setStatus(ExecutionTrace.ExecutionStatus.COMPLETED);
        assertEquals(ExecutionTrace.ExecutionStatus.COMPLETED, trace.getStatus());
    }

    @Test
    void testAgentRunStateManagement() {
        DAG dag = UrlShorteningDAGBuilder.buildUrlShorteningDAG("test-3", "https://example.com");

        AgentRun run = AgentRun.builder()
                .id("test-run-3")
                .dag(dag)
                .status(AgentRun.RunStatus.PENDING)
                .build();

        assertEquals(AgentRun.RunStatus.PENDING, run.getStatus());

        run.setStatus(AgentRun.RunStatus.RUNNING);
        assertEquals(AgentRun.RunStatus.RUNNING, run.getStatus());

        run.setStatus(AgentRun.RunStatus.COMPLETED);
        assertEquals(AgentRun.RunStatus.COMPLETED, run.getStatus());
    }

    @Test
    void testAgentOutputStorage() {
        DAG dag = UrlShorteningDAGBuilder.buildUrlShorteningDAG("test-4", "https://example.com");

        AgentRun run = AgentRun.builder()
                .id("test-run-4")
                .dag(dag)
                .build();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("requirements", "analyzed");
        output.put("status", "success");

        run.setAgentOutput("requirement-agent", output);

        Map<String, Object> retrieved = run.getAgentOutput("requirement-agent");
        assertNotNull(retrieved);
        assertEquals("analyzed", retrieved.get("requirements"));
    }

    @Test
    void testDataConsistency() {
        for (int i = 0; i < 5; i++) {
            ExecutionTrace trace = ExecutionTrace.builder()
                    .workflowId("consistency-test-" + i)
                    .originalUrl("https://example.com/" + i)
                    .status(ExecutionTrace.ExecutionStatus.COMPLETED)
                    .shortCode("code" + i)
                    .build();

            traceRepository.save(trace);
        }

        assertEquals(5, traceRepository.findAll().size());

        List<ExecutionTrace> all = traceRepository.findAll();
        for (ExecutionTrace trace : all) {
            assertNotNull(trace.getWorkflowId());
            assertNotNull(trace.getOriginalUrl());
            assertNotNull(trace.getStatus());
        }
    }
}