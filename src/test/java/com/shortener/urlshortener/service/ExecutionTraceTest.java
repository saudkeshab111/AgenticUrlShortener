package com.shortener.urlshortener.service;

import com.shortener.urlshortener.model.ExecutionTrace;
import com.shortener.urlshortener.repository.ExecutionTraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExecutionTraceTest {

    @Autowired
    private ExecutionTraceService traceService;

    @Autowired
    private ExecutionTraceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testCreateTrace() {
        ExecutionTrace trace = traceService.createTrace("wf-001", "https://example.com");

        assertNotNull(trace);
        assertEquals("wf-001", trace.getWorkflowId());
        assertEquals("https://example.com", trace.getOriginalUrl());
        assertEquals(ExecutionTrace.ExecutionStatus.RUNNING, trace.getStatus());
    }

    @Test
    void testCompleteTrace() {
        traceService.createTrace("wf-002", "https://github.com");

        Map<String, Object> outputs = Map.of(
                "requirement", "analyzed",
                "architecture", "designed",
                "security", "reviewed"
        );

        ExecutionTrace completed = traceService.completeTrace("wf-002", "abc123", 1500L, outputs);

        assertEquals(ExecutionTrace.ExecutionStatus.COMPLETED, completed.getStatus());
        assertEquals("abc123", completed.getShortCode());
        assertEquals(1500L, completed.getExecutionTimeMs());
    }

    @Test
    void testFailTrace() {
        traceService.createTrace("wf-003", "https://test.com");

        ExecutionTrace failed = traceService.failTrace("wf-003", "Invalid URL");

        assertEquals(ExecutionTrace.ExecutionStatus.FAILED, failed.getStatus());
        assertEquals("Invalid URL", failed.getErrorMessage());
    }

    @Test
    void testBlockTrace() {
        traceService.createTrace("wf-004", "https://block.com");

        ExecutionTrace blocked = traceService.blockTrace("wf-004", "Gate rejection");

        assertEquals(ExecutionTrace.ExecutionStatus.BLOCKED, blocked.getStatus());
        assertEquals("Gate rejection", blocked.getErrorMessage());
    }

    @Test
    void testGetTrace() {
        traceService.createTrace("wf-005", "https://get.com");

        ExecutionTrace retrieved = traceService.getTrace("wf-005");

        assertNotNull(retrieved);
        assertEquals("wf-005", retrieved.getWorkflowId());
    }

    @Test
    void testGetTracesByStatus() {
        traceService.createTrace("wf-006", "https://running1.com");
        traceService.createTrace("wf-007", "https://running2.com");

        List<ExecutionTrace> running = traceService.getTracesByStatus(ExecutionTrace.ExecutionStatus.RUNNING);

        assertEquals(2, running.size());
    }

    @Test
    void testGetTracesByUrl() {
        traceService.createTrace("wf-008", "https://same.com");
        traceService.createTrace("wf-009", "https://same.com");

        List<ExecutionTrace> traces = traceService.getTracesByUrl("https://same.com");

        assertEquals(2, traces.size());
    }

    @Test
    void testTraceNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            traceService.getTrace("non-existent");
        });
    }
}