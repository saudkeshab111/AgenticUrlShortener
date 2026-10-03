package com.shortener.orchestrator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Directed Acyclic Graph (DAG) representing the orchestration workflow.
 * Contains nodes and defines execution order and dependencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DAG {

    private String id;
    private String name;

    @Builder.Default
    private List<DAGNode> nodes = new ArrayList<>();

    /**
     * Get nodes in topological order (respecting dependencies)
     */
    public List<DAGNode> getTopologicalOrder() {
        List<DAGNode> sorted = new ArrayList<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // Initialize in-degree for all nodes
        for (DAGNode node : nodes) {
            inDegree.put(node.getId(), 0);
        }

        // Calculate in-degree (count dependencies)
        for (DAGNode node : nodes) {
            if (node.getDependsOn() != null && !node.getDependsOn().isEmpty()) {
                inDegree.put(node.getId(), inDegree.getOrDefault(node.getId(), 0) + 1);
            }
        }

        // Start with nodes that have no dependencies
        Queue<DAGNode> queue = new LinkedList<>();
        for (DAGNode node : nodes) {
            if (inDegree.get(node.getId()) == 0) {
                queue.offer(node);
            }
        }

        // Process queue
        while (!queue.isEmpty()) {
            DAGNode current = queue.poll();
            sorted.add(current);

            // Find nodes that depend on current node and reduce their in-degree
            for (DAGNode node : nodes) {
                if (node.getDependsOn() != null &&
                        node.getDependsOn().equals(current.getId())) {
                    inDegree.put(node.getId(), inDegree.get(node.getId()) - 1);
                    if (inDegree.get(node.getId()) == 0) {
                        queue.offer(node);
                    }
                }
            }
        }

        return sorted;
    }

    /**
     * Add a node to the DAG
     */
    public void addNode(DAGNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        nodes.add(node);
    }

    /**
     * Get a node by ID
     */
    public DAGNode getNodeById(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Validate DAG has no cycles
     */
    public boolean isValid() {
        return getTopologicalOrder().size() == nodes.size();
    }

    /**
     * Get execution order (respecting dependencies)
     */
    public List<DAGNode> getExecutionOrder() {
        return getTopologicalOrder();
    }
}