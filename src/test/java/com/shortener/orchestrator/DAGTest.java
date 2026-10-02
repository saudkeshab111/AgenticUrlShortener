package com.shortener.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DAGTest {

    private DAG dag;

    @BeforeEach
    void setUp() {
        dag = new DAG("test-dag");
    }

    @Test
    void testAddNode() {
        DAGNode node = new DAGNode("node1", "Requirement Agent", "REQUIREMENT");
        dag.addNode(node);

        assertNotNull(dag.getNode("node1"));
        assertEquals("Requirement Agent", dag.getNode("node1").getName());
    }

    @Test
    void testTopologicalSort() {
        DAGNode node1 = new DAGNode("node1", "First", "REQUIREMENT");
        DAGNode node2 = new DAGNode("node2", "Second", "ARCHITECTURE");
        node2.addDependency("node1");

        dag.addNode(node1);
        dag.addNode(node2);

        List<DAGNode> sorted = dag.getTopologicalOrder();

        assertEquals(2, sorted.size());
        assertEquals("node1", sorted.get(0).getId());
        assertEquals("node2", sorted.get(1).getId());
    }

    @Test
    void testGetExecutableNodes() {
        DAGNode node1 = new DAGNode("node1", "First", "REQUIREMENT");
        DAGNode node2 = new DAGNode("node2", "Second", "ARCHITECTURE");
        node2.addDependency("node1");

        dag.addNode(node1);
        dag.addNode(node2);

        Map<String, DAGNode.ExecutionState> states = new HashMap<>();
        states.put("node1", DAGNode.ExecutionState.PASSED);
        states.put("node2", DAGNode.ExecutionState.PENDING);

        List<DAGNode> executable = dag.getExecutableNodes(states);

        assertEquals(1, executable.size());
        assertEquals("node2", executable.get(0).getId());
    }
}