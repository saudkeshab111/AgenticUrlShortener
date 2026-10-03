package com.shortener.urlshortener.api;

import com.shortener.orchestrator.UrlShorteningOrchestrator;
import com.shortener.urlshortener.model.ExecutionTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class OrchestratorUrlShorteningController {

    private static final String GENERIC_ERROR =
            "An unexpected error occurred. Check server logs for details.";

    private final UrlShorteningOrchestrator orchestrator;

    public OrchestratorUrlShorteningController(UrlShorteningOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * POST /api/v1/urls/orchestrated
     * 201 = workflow completed, 400 = invalid input or workflow failed validation, 500 = unexpected error.
     */
    @PostMapping("/urls/orchestrated")
    public ResponseEntity<Map<String, Object>> createShortUrlOrchestrated(
            @RequestBody Map<String, Object> request) {

        log.info("Received orchestrated URL shortening request: {}", request);

        String originalUrl;
        int expirationDays;
        try {
            originalUrl = parseOriginalUrl(request.get("originalUrl"));
            expirationDays = parseExpirationDays(request.get("expirationDays"));
        } catch (IllegalArgumentException e) {
            return failed(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        try {
            Map<String, Object> result =
                    orchestrator.executeUrlShorteningWorkflow(originalUrl, expirationDays);

            if (result != null && "FAILED".equals(String.valueOf(result.get("status")))) {
                log.warn("Workflow failed for URL {}: {}", originalUrl, result.get("error"));
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid URL provided: {}", e.getMessage());
            return failed(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error during orchestration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "error", GENERIC_ERROR));
        }
    }

    /**
     * GET /api/v1/orchestration/traces/{workflowId}
     */
    @GetMapping("/orchestration/traces/{workflowId}")
    public ResponseEntity<Map<String, Object>> getExecutionTrace(@PathVariable String workflowId) {
        try {
            log.info("Fetching execution trace for workflow: {}", workflowId);
            return ResponseEntity.ok(orchestrator.getWorkflowStatus(workflowId));

        } catch (IllegalArgumentException e) {
            log.warn("Trace not found: {}", workflowId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "NOT_FOUND", "error", "Trace not found: " + workflowId));
        } catch (Exception e) {
            log.error("Error retrieving execution trace: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "error", GENERIC_ERROR));
        }
    }

    /**
     * GET /api/v1/orchestration/traces?status=COMPLETED
     * Defaults to COMPLETED. Invalid status values return 400 with the allowed list.
     */
    @GetMapping("/orchestration/traces")
    public ResponseEntity<?> getTracesByStatus(
            @RequestParam(defaultValue = "COMPLETED") String status) {

        String normalized = status.trim().toUpperCase();
        try {
            ExecutionTrace.ExecutionStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return failed(HttpStatus.BAD_REQUEST, "Invalid status '" + status + "'. Allowed values: "
                    + Arrays.toString(ExecutionTrace.ExecutionStatus.values()));
        }

        try {
            log.info("Fetching traces by status: {}", normalized);
            List<Map<String, Object>> traces = orchestrator.getTracesByStatus(normalized);
            return ResponseEntity.ok(traces);
        } catch (Exception e) {
            log.error("Error retrieving traces: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "error", GENERIC_ERROR));
        }
    }

    private static String parseOriginalUrl(Object value) {
        if (!(value instanceof String url) || url.isBlank()) {
            throw new IllegalArgumentException("originalUrl is required");
        }
        return url;
    }

    private static int parseExpirationDays(Object value) {
        if (value == null) {
            return 0;
        }
        int days;
        if (value instanceof Number number) {
            days = number.intValue();
        } else if (value instanceof String text && !text.isBlank()) {
            try {
                days = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("expirationDays must be a whole number");
            }
        } else {
            throw new IllegalArgumentException("expirationDays must be a whole number");
        }
        if (days < 0) {
            throw new IllegalArgumentException("expirationDays cannot be negative");
        }
        return days;
    }

    private static ResponseEntity<Map<String, Object>> failed(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("status", "FAILED", "error", Objects.toString(message, "Invalid request")));
    }
}