package com.shortener.orchestrator;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Directed Acyclic Graph of orchestration tasks.
 * Handles topological sorting for execution order.
 */
@Data
@NoArgsConstructor
public class DAG {

    private String id;
    private List<DAGNode> nodes = new ArrayList<>();
    private Map<String, DAGNode> nodeMap = new HashMap<>();

    public DAG(String id) {
        this.id = id;
    }

    public void addNode(DAGNode node) {
        if (!this.nodeMap.containsKey(node.getId())) {
            this.nodes.add(node);
            this.nodeMap.put(node.getId(), node);
        }
    }

    public DAGNode getNode(String nodeId) {
        return this.nodeMap.get(nodeId);
    }

    public List<DAGNode> getTopologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();

        // In-degree = number of dependencies each node is waiting on
        for (DAGNode node : this.nodes) {
            inDegree.put(node.getId(), node.getDependsOn().size());
        }

        // Start with nodes that have no dependencies
        Queue<DAGNode> queue = new LinkedList<>();
        for (DAGNode node : this.nodes) {
            if (inDegree.get(node.getId()) == 0) {
                queue.offer(node);
            }
        }

        List<DAGNode> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            DAGNode node = queue.poll();
            sorted.add(node);

            // Unlock nodes that depended on this one
            for (DAGNode other : this.nodes) {
                if (other.getDependsOn().contains(node.getId())) {
                    int newDegree = inDegree.get(other.getId()) - 1;
                    inDegree.put(other.getId(), newDegree);
                    if (newDegree == 0) {
                        queue.offer(other);
                    }
                }
            }
        }

        if (sorted.size() != this.nodes.size()) {
            throw new IllegalStateException("DAG contains a cycle!");
        }

        return sorted;
    }

    public List<DAGNode> getExecutableNodes(Map<String, DAGNode.ExecutionState> nodeStates) {
        return this.nodes.stream()
                .filter(node -> nodeStates.getOrDefault(node.getId(), DAGNode.ExecutionState.PENDING)
                        == DAGNode.ExecutionState.PENDING)
                .filter(node -> node.canExecute(nodeStates))
                .collect(Collectors.toList());
    }
}