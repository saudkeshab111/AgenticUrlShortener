package com.shortener.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DAGTest {

    private DAG dag;

    @BeforeEach
    void setUp() {
        dag = DAG.builder()
                .id("test-dag")
                .name("Test DAG")
                .build();
    }

    @Test
    void testAddNode() {
        DAGNode node = DAGNode.builder()
                .id("node1")
                .name("Requirement Agent")
                .agentType(AgentType.REQUIREMENT)
                .dependsOn(null)
                .build();

        dag.addNode(node);

        assertNotNull(dag.getNodeById("node1"));
        assertEquals("Requirement Agent", dag.getNodeById("node1").getName());
    }

    @Test
    void testTopologicalSort() {
        DAGNode node1 = DAGNode.builder()
                .id("node1")
                .name("First")
                .agentType(AgentType.REQUIREMENT)
                .dependsOn(null)
                .build();

        DAGNode node2 = DAGNode.builder()
                .id("node2")
                .name("Second")
                .agentType(AgentType.ARCHITECTURE)
                .dependsOn("node1")
                .build();

        dag.addNode(node1);
        dag.addNode(node2);

        List<DAGNode> sorted = dag.getTopologicalOrder();

        assertEquals(2, sorted.size());
        assertEquals("node1", sorted.get(0).getId());
        assertEquals("node2", sorted.get(1).getId());
    }

    @Test
    void testExecutionOrder() {
        DAGNode node1 = DAGNode.builder()
                .id("node1")
                .name("First")
                .agentType(AgentType.REQUIREMENT)
                .dependsOn(null)
                .build();

        DAGNode node2 = DAGNode.builder()
                .id("node2")
                .name("Second")
                .agentType(AgentType.ARCHITECTURE)
                .dependsOn("node1")
                .build();

        DAGNode node3 = DAGNode.builder()
                .id("node3")
                .name("Third")
                .agentType(AgentType.SECURITY)
                .dependsOn("node2")
                .build();

        dag.addNode(node1);
        dag.addNode(node2);
        dag.addNode(node3);

        List<DAGNode> order = dag.getExecutionOrder();

        assertEquals(3, order.size());
        assertEquals("node1", order.get(0).getId());
        assertEquals("node2", order.get(1).getId());
        assertEquals("node3", order.get(2).getId());
    }

    @Test
    void testDAGValidation() {
        DAGNode node1 = DAGNode.builder()
                .id("node1")
                .name("First")
                .agentType(AgentType.REQUIREMENT)
                .dependsOn(null)
                .build();

        DAGNode node2 = DAGNode.builder()
                .id("node2")
                .name("Second")
                .agentType(AgentType.ARCHITECTURE)
                .dependsOn("node1")
                .build();

        dag.addNode(node1);
        dag.addNode(node2);

        assertTrue(dag.isValid());
    }

    @Test
    void testGetNodeById() {
        DAGNode node = DAGNode.builder()
                .id("test-node")
                .name("Test Node")
                .agentType(AgentType.IMPLEMENTATION)
                .dependsOn(null)
                .build();

        dag.addNode(node);

        DAGNode retrieved = dag.getNodeById("test-node");
        assertNotNull(retrieved);
        assertEquals("test-node", retrieved.getId());
        assertEquals("Test Node", retrieved.getName());
    }

    @Test
    void testGetNodeByIdNotFound() {
        DAGNode retrieved = dag.getNodeById("non-existent");
        assertNull(retrieved);
    }

    @Test
    void testComplexDAG() {
        // Build a 5-node DAG like the URL shortening workflow
        DAGNode req = DAGNode.builder()
                .id("requirement")
                .name("Requirement")
                .agentType(AgentType.REQUIREMENT)
                .dependsOn(null)
                .build();

        DAGNode arch = DAGNode.builder()
                .id("architecture")
                .name("Architecture")
                .agentType(AgentType.ARCHITECTURE)
                .dependsOn("requirement")
                .build();

        DAGNode sec = DAGNode.builder()
                .id("security")
                .name("Security")
                .agentType(AgentType.SECURITY)
                .dependsOn("architecture")
                .build();

        DAGNode impl = DAGNode.builder()
                .id("implementation")
                .name("Implementation")
                .agentType(AgentType.IMPLEMENTATION)
                .dependsOn("security")
                .build();

        DAGNode test = DAGNode.builder()
                .id("test")
                .name("Testing")
                .agentType(AgentType.TEST)
                .dependsOn("implementation")
                .build();

        dag.addNode(req);
        dag.addNode(arch);
        dag.addNode(sec);
        dag.addNode(impl);
        dag.addNode(test);

        List<DAGNode> order = dag.getExecutionOrder();
        assertEquals(5, order.size());
        assertEquals("requirement", order.get(0).getId());
        assertEquals("test", order.get(4).getId());
        assertTrue(dag.isValid());
    }
}