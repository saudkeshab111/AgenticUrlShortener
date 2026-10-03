package com.shortener.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "execution_traces")
public class ExecutionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", unique = true, nullable = false)
    private String workflowId;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "short_code")
    private String shortCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExecutionStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "agent_outputs", columnDefinition = "TEXT")
    private String agentOutputsJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        BLOCKED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}