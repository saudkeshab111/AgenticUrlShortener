package com.shortener.orchestrator.recovery;

import lombok.Data;
import java.util.*;

/**
 * Rollback strategy for reverting failed orchestration runs.
 * Tracks what was changed and can undo it in reverse order.
 */
@Data
public class RollbackStrategy {

    private List<RollbackAction> actions = new ArrayList<>();
    private RollbackStatus status = RollbackStatus.NOT_STARTED;

    public enum RollbackStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public static class RollbackAction {
        public String nodeId;
        public String description;
        public Runnable undoAction;

        public RollbackAction(String nodeId, String description) {
            this.nodeId = nodeId;
            this.description = description;
        }
    }

    /**
     * Record an action that can be rolled back.
     */
    public void recordAction(String nodeId, String description) {
        RollbackAction action = new RollbackAction(nodeId, description);
        this.actions.add(0, action); // Add to front for LIFO
    }

    /**
     * Execute rollback in reverse order.
     */
    public void rollback() {
        this.status = RollbackStatus.IN_PROGRESS;
        for (RollbackAction action : actions) {
            try {
                if (action.undoAction != null) {
                    action.undoAction.run();
                }
            } catch (Exception e) {
                this.status = RollbackStatus.FAILED;
                throw new RuntimeException("Rollback failed for: " + action.nodeId, e);
            }
        }
        this.status = RollbackStatus.COMPLETED;
    }

    public int getPendingActions() {
        return this.actions.size();
    }
}