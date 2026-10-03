package com.shortener.urlshortener.service;

import com.shortener.urlshortener.model.ExecutionTrace;
import com.shortener.urlshortener.repository.ExecutionTraceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class ExecutionTraceService {

    private final ExecutionTraceRepository repository;

    public ExecutionTraceService(ExecutionTraceRepository repository) {
        this.repository = repository;
    }

    /**
     * Create and persist execution trace
     */
    public ExecutionTrace createTrace(String workflowId, String originalUrl) {
        ExecutionTrace trace = ExecutionTrace.builder()
                .workflowId(workflowId)
                .originalUrl(originalUrl)
                .status(ExecutionTrace.ExecutionStatus.RUNNING)
                .startTime(Instant.now())
                .build();

        ExecutionTrace saved = repository.save(trace);
        log.info("Created execution trace: {}", workflowId);
        return saved;
    }

    /**
     * Complete execution trace with success
     */
    public ExecutionTrace completeTrace(String workflowId, String shortCode,
                                        Long executionTimeMs, Map<String, Object> agentOutputs) {
        ExecutionTrace trace = repository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Trace not found: " + workflowId));

        trace.setStatus(ExecutionTrace.ExecutionStatus.COMPLETED);
        trace.setShortCode(shortCode);
        trace.setExecutionTimeMs(executionTimeMs);
        trace.setEndTime(Instant.now());

        if (agentOutputs != null) {
            trace.setAgentOutputsJson(convertToJson(agentOutputs));
        }

        ExecutionTrace saved = repository.save(trace);
        log.info("Completed execution trace: {} in {}ms", workflowId, executionTimeMs);
        return saved;
    }

    /**
     * Mark trace as failed
     */
    public ExecutionTrace failTrace(String workflowId, String errorMessage) {
        ExecutionTrace trace = repository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Trace not found: " + workflowId));

        trace.setStatus(ExecutionTrace.ExecutionStatus.FAILED);
        trace.setErrorMessage(errorMessage);
        trace.setEndTime(Instant.now());

        ExecutionTrace saved = repository.save(trace);
        log.warn("Failed execution trace: {} - {}", workflowId, errorMessage);
        return saved;
    }

    /**
     * Mark trace as blocked
     */
    public ExecutionTrace blockTrace(String workflowId, String reason) {
        ExecutionTrace trace = repository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Trace not found: " + workflowId));

        trace.setStatus(ExecutionTrace.ExecutionStatus.BLOCKED);
        trace.setErrorMessage(reason);
        trace.setEndTime(Instant.now());

        ExecutionTrace saved = repository.save(trace);
        log.warn("Blocked execution trace: {} - {}", workflowId, reason);
        return saved;
    }

    /**
     * Get trace by workflow ID
     */
    public ExecutionTrace getTrace(String workflowId) {
        return repository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Trace not found: " + workflowId));
    }

    /**
     * Get all traces by status
     */
    public List<ExecutionTrace> getTracesByStatus(ExecutionTrace.ExecutionStatus status) {
        return repository.findByStatus(status);
    }

    /**
     * Get all traces for a URL
     */
    public List<ExecutionTrace> getTracesByUrl(String originalUrl) {
        return repository.findByOriginalUrl(originalUrl);
    }

    /**
     * Get traces in time range
     */
    public List<ExecutionTrace> getTracesByTimeRange(Instant start, Instant end) {
        return repository.findByCreatedAtBetween(start, end);
    }

    /**
     * Convert object to JSON string
     */
    private String convertToJson(Object obj) {
        try {
            // Simple JSON conversion using toString()
            return obj.toString();
        } catch (Exception e) {
            log.error("Error converting to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}