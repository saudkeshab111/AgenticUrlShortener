package com.shortener.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrchestrationResponse {

    private String workflowId;
    private String shortCode;
    private String originalUrl;
    private String status;
    private Long executionTimeMs;
    private Instant createdAt;
    private Instant expiresAt;
    private String error;
    private ExecutionSummary executionTrace;
    private Map<String, Object> agentOutputs;
}