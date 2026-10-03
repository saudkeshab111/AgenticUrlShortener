package com.shortener.orchestrator;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds the DAG for orchestrated URL shortening.
 * Flow: Requirements → Architecture → Security → Implementation → Test
 */
@Slf4j
public class UrlShorteningDAGBuilder {

    /**
     * Build URL shortening DAG with proper dependency chain
     */
    public static DAG buildUrlShorteningDAG(String workflowId, String originalUrl) {
        log.info("[{}] Building URL shortening DAG for: {}", workflowId, originalUrl);

        // Create DAG
        DAG dag = DAG.builder()
                .id(workflowId)
                .name("URL Shortening Orchestration")
                .build();

        // Node 1: Requirement Analysis (entry point, no dependencies)
        DAGNode requirementNode = DAGNode.builder()
                .id("requirement-agent")
                .name("Requirement Analysis")
                .agentType(AgentType.REQUIREMENT)
                .dependsOn(null)
                .build();

        // Node 2: Architecture Design (depends on requirement)
        DAGNode architectureNode = DAGNode.builder()
                .id("architecture-agent")
                .name("Architecture Design")
                .agentType(AgentType.ARCHITECTURE)
                .dependsOn("requirement-agent")
                .build();

        // Node 3: Security Review (depends on architecture)
        DAGNode securityNode = DAGNode.builder()
                .id("security-agent")
                .name("Security Review")
                .agentType(AgentType.SECURITY)
                .dependsOn("architecture-agent")
                .build();

        // Node 4: Implementation (depends on security)
        DAGNode implementationNode = DAGNode.builder()
                .id("implementation-agent")
                .name("Implementation")
                .agentType(AgentType.IMPLEMENTATION)
                .dependsOn("security-agent")
                .build();

        // Node 5: Testing (depends on implementation)
        DAGNode testNode = DAGNode.builder()
                .id("test-agent")
                .name("Testing")
                .agentType(AgentType.TEST)
                .dependsOn("implementation-agent")
                .build();

        // Add nodes to DAG
        dag.addNode(requirementNode);
        dag.addNode(architectureNode);
        dag.addNode(securityNode);
        dag.addNode(implementationNode);
        dag.addNode(testNode);

        // Validate DAG
        if (!dag.isValid()) {
            log.warn("[{}] DAG validation failed - possible cycle detected", workflowId);
        }

        log.info("[{}] DAG built successfully with {} nodes", workflowId, dag.getNodes().size());
        return dag;
    }
}