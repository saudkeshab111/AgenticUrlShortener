package com.shortener.urlshortener.repository;

import com.shortener.urlshortener.model.ExecutionTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionTraceRepository extends JpaRepository<ExecutionTrace, Long> {

    /**
     * Find trace by workflow ID
     */
    Optional<ExecutionTrace> findByWorkflowId(String workflowId);

    /**
     * Find all traces by status
     */
    List<ExecutionTrace> findByStatus(ExecutionTrace.ExecutionStatus status);

    /**
     * Find traces by URL
     */
    List<ExecutionTrace> findByOriginalUrl(String originalUrl);

    /**
     * Find traces created within time range
     */
    List<ExecutionTrace> findByCreatedAtBetween(Instant start, Instant end);

    /**
     * Find failed traces
     */
    List<ExecutionTrace> findByStatusOrderByCreatedAtDesc(ExecutionTrace.ExecutionStatus status);
}