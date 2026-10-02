package com.shortener.orchestrator.recovery;

import lombok.Data;

/**
 * Retry strategy for failed nodes.
 * Supports bounded retries with exponential backoff.
 */
@Data
public class RetryStrategy {

    private int maxRetries = 3;
    private long initialDelayMs = 100;
    private double backoffMultiplier = 2.0;
    private long maxDelayMs = 10000;

    public RetryStrategy() {
    }

    public RetryStrategy(int maxRetries, long initialDelayMs) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
    }

    /**
     * Calculate delay for a given retry attempt.
     */
    public long calculateDelay(int attemptNumber) {
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber - 1));
        return Math.min(delay, maxDelayMs);
    }

    /**
     * Check if retry is allowed.
     */
    public boolean canRetry(int currentAttempt) {
        return currentAttempt < maxRetries;
    }

    /**
     * Get next attempt number.
     */
    public int nextAttempt(int currentAttempt) {
        return currentAttempt + 1;
    }
}