package com.shortener.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.shortener.orchestrator.recovery.RetryStrategy;

import static org.junit.jupiter.api.Assertions.*;

class RetryStrategyTest {

    private RetryStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RetryStrategy(3, 100);
    }

    @Test
    void testCanRetry() {
        assertTrue(strategy.canRetry(1));
        assertTrue(strategy.canRetry(2));
        assertFalse(strategy.canRetry(3));
    }

    @Test
    void testCalculateExponentialBackoff() {
        long delay1 = strategy.calculateDelay(1);
        long delay2 = strategy.calculateDelay(2);
        long delay3 = strategy.calculateDelay(3);

        assertEquals(100, delay1);
        assertEquals(200, delay2);
        assertEquals(400, delay3);
    }

    @Test
    void testNextAttempt() {
        int next = strategy.nextAttempt(1);
        assertEquals(2, next);
    }
}