package com.shortener.orchestrator;

import com.shortener.orchestrator.agents.*;
import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.service.ShortUrlService;
import com.shortener.urlshortener.service.ExecutionTraceService;
import com.shortener.urlshortener.model.ExecutionTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class UrlShorteningOrchestrator {

    private final OrchestratorEngine orchestratorEngine;
    private final ShortUrlService shortUrlService;
    private final ExecutionTraceService traceService;
    private final RequirementAgent requirementAgent;
    private final ArchitectureAgent architectureAgent;
    private final SecurityAgent securityAgent;
    private final ImplementationAgent implementationAgent;
    private final TestAgent testAgent;

    public UrlShorteningOrchestrator(
            OrchestratorEngine orchestratorEngine,
            ShortUrlService shortUrlService,
            ExecutionTraceService traceService,
            RequirementAgent requirementAgent,
            ArchitectureAgent architectureAgent,
            SecurityAgent securityAgent,
            ImplementationAgent implementationAgent,
            TestAgent testAgent) {
        this.orchestratorEngine = orchestratorEngine;
        this.shortUrlService = shortUrlService;
        this.traceService = traceService;
        this.requirementAgent = requirementAgent;
        this.architectureAgent = architectureAgent;
        this.securityAgent = securityAgent;
        this.implementationAgent = implementationAgent;
        this.testAgent = testAgent;
    }

    /**
     * Execute orchestrated URL shortening workflow with persistence
     */
    public Map<String, Object> executeUrlShorteningWorkflow(String originalUrl, int expirationDays) {
        String workflowId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // Create execution trace
        ExecutionTrace trace = traceService.createTrace(workflowId, originalUrl);

        try {
            log.info("[{}] Starting orchestrated URL shortening for: {}", workflowId, originalUrl);

            // Build DAG
            DAG dag = UrlShorteningDAGBuilder.buildUrlShorteningDAG(workflowId, originalUrl);

            // Create agent run
            AgentRun run = AgentRun.builder()
                    .id(workflowId)
                    .dag(dag)
                    .status(AgentRun.RunStatus.RUNNING)
                    .startTime(Instant.now())
                    .build();

            // Execute orchestration
            AgentRun completedRun = orchestratorEngine.execute(run, originalUrl);

            // Check if execution was blocked by gate
            if (completedRun.getStatus() == AgentRun.RunStatus.BLOCKED) {
                long executionTime = System.currentTimeMillis() - startTime;
                traceService.blockTrace(workflowId, "Execution blocked by gate");

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("workflowId", workflowId);
                result.put("status", "BLOCKED");
                result.put("error", "Execution blocked by gate");
                result.put("executionTimeMs", executionTime);
                return result;
            }

            // Check if execution failed
            if (completedRun.getStatus() == AgentRun.RunStatus.FAILED) {
                long executionTime = System.currentTimeMillis() - startTime;
                traceService.failTrace(workflowId, completedRun.getErrorMessage());

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("workflowId", workflowId);
                result.put("status", "FAILED");
                result.put("error", completedRun.getErrorMessage());
                result.put("executionTimeMs", executionTime);
                return result;
            }

            // Extract agent outputs
            Map<String, Object> requirementOutput = completedRun.getAgentOutput("requirement-agent");
            Map<String, Object> architectureOutput = completedRun.getAgentOutput("architecture-agent");
            Map<String, Object> securityOutput = completedRun.getAgentOutput("security-agent");
            Map<String, Object> implementationOutput = completedRun.getAgentOutput("implementation-agent");
            Map<String, Object> testOutput = completedRun.getAgentOutput("test-agent");

            // Create shortened URL based on orchestration
            ShortUrl shortUrl = shortUrlService.createShortUrl(originalUrl, expirationDays);

            // Compile results
            long executionTime = System.currentTimeMillis() - startTime;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("workflowId", workflowId);
            result.put("shortUrl", shortUrl);
            result.put("shortCode", shortUrl.getCode());
            result.put("originalUrl", originalUrl);
            result.put("createdAt", shortUrl.getCreatedAt());
            result.put("expiresAt", shortUrl.getExpiresAt());
            result.put("executionTimeMs", executionTime);
            result.put("status", "SUCCESS");

            // Agent outputs
            Map<String, Object> agents = new LinkedHashMap<>();
            agents.put("requirements", requirementOutput);
            agents.put("architecture", architectureOutput);
            agents.put("security", securityOutput);
            agents.put("implementation", implementationOutput);
            agents.put("testing", testOutput);
            result.put("agentOutputs", agents);

            // Persist trace
            traceService.completeTrace(workflowId, shortUrl.getCode(), executionTime, agents);

            log.info("[{}] Workflow completed in {}ms", workflowId, executionTime);
            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] Workflow failed after {}ms: {}", workflowId, executionTime, e.getMessage(), e);

            // Persist failure
            traceService.failTrace(workflowId, e.getMessage());

            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("workflowId", workflowId);
            errorResult.put("status", "FAILED");
            errorResult.put("error", e.getMessage());
            errorResult.put("executionTimeMs", executionTime);
            return errorResult;
        }
    }

    /**
     * Get workflow status with trace
     */
    public Map<String, Object> getWorkflowStatus(String workflowId) {
        ExecutionTrace trace = traceService.getTrace(workflowId);

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("workflowId", workflowId);
        status.put("status", trace.getStatus().toString());
        status.put("originalUrl", trace.getOriginalUrl());
        status.put("shortCode", trace.getShortCode());
        status.put("executionTimeMs", trace.getExecutionTimeMs());
        status.put("startTime", trace.getStartTime());
        status.put("endTime", trace.getEndTime());
        status.put("errorMessage", trace.getErrorMessage());
        status.put("createdAt", trace.getCreatedAt());
        status.put("updatedAt", trace.getUpdatedAt());

        return status;
    }

    /**
     * Get execution traces by status
     */
    public List<Map<String, Object>> getTracesByStatus(String statusStr) {
        ExecutionTrace.ExecutionStatus status = ExecutionTrace.ExecutionStatus.valueOf(statusStr.toUpperCase());
        List<ExecutionTrace> traces = traceService.getTracesByStatus(status);

        List<Map<String, Object>> results = new ArrayList<>();
        for (ExecutionTrace trace : traces) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("workflowId", trace.getWorkflowId());
            map.put("status", trace.getStatus().toString());
            map.put("originalUrl", trace.getOriginalUrl());
            map.put("shortCode", trace.getShortCode());
            map.put("executionTimeMs", trace.getExecutionTimeMs());
            results.add(map);
        }

        return results;
    }
}