package com.shortener.orchestrator.recovery;

import lombok.Data;

/**
 * Handles safe termination of orchestration runs.
 * Ensures cleanup happens even if a prohibited action is triggered.
 */
@Data
public class SafeStopHandler {

    private StopReason reason;
    private String message;
    private boolean cleanupComplete = false;

    public enum StopReason {
        PROHIBITED_ACTION,
        CRITICAL_ERROR,
        MANUAL_STOP,
        RESOURCE_EXHAUSTION,
        TIMEOUT
    }

    public SafeStopHandler(StopReason reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    /**
     * Execute safe stop sequence.
     */
    public void stop(RollbackStrategy rollbackStrategy) {
        try {
            // Rollback any changes
            if (rollbackStrategy != null && rollbackStrategy.getPendingActions() > 0) {
                rollbackStrategy.rollback();
            }

            // Log stop event
            logStop();

            // Mark cleanup complete
            this.cleanupComplete = true;
        } catch (Exception e) {
            throw new RuntimeException("Safe stop failed: " + message, e);
        }
    }

    private void logStop() {
        System.err.println("[SAFE_STOP] " + reason + ": " + message);
    }

    public boolean isStopped() {
        return this.cleanupComplete;
    }
}