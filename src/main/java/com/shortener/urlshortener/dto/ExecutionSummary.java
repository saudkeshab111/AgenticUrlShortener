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
public class ExecutionSummary {

    private String workflowId;
    private String originalUrl;
    private String shortCode;
    private String status;
    private Long executionTimeMs;
    private Instant startTime;
    private Instant endTime;
    private String errorMessage;
    private Map<String, Object> agentOutputs;
    private Instant createdAt;
}