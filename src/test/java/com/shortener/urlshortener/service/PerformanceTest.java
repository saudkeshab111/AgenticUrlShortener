package com.shortener.urlshortener.service;

import com.shortener.urlshortener.model.ShortUrl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class PerformanceTest {

    /** Generous limits: these are smoke checks, not benchmarks, and must not fail on slower machines. */
    private static final long CREATE_LIMIT_MS = 5000;
    private static final long CONCURRENT_LIMIT_MS = 10000;
    private static final long CLICK_LIMIT_MS = 5000;

    @Autowired
    private ShortUrlService shortUrlService;

    @BeforeEach
    void setUp() {
        log.info("PerformanceTest setup completed");
    }

    @Test
    void testShortUrlCreationPerformance() {
        log.info("Starting testShortUrlCreationPerformance");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            ShortUrl url = shortUrlService.createShortUrl("https://example.com/" + i, 30);
            assertNotNull(url.getCode(), "Short code should not be null for iteration " + i);
        }

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("Created 100 short URLs in {}ms", totalTime);
        assertTrue(totalTime < CREATE_LIMIT_MS,
                "Should create 100 URLs in under " + CREATE_LIMIT_MS + "ms, but took: " + totalTime + "ms");
    }

    @Test
    void testConcurrentShortUrlCreation() throws InterruptedException {
        log.info("Starting testConcurrentShortUrlCreation");

        int threadCount = 10;
        int urlsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < urlsPerThread; j++) {
                        ShortUrl url = shortUrlService.createShortUrl(
                                "https://concurrent.com/" + threadId + "/" + j, 30);
                        if (url.getCode() == null) {
                            throw new IllegalStateException("Null code in thread " + threadId);
                        }
                    }
                } catch (Throwable t) {
                    log.error("Error in thread {}", threadId, t);
                    errors.add(t);
                }
            });
        }

        executor.shutdown();
        boolean completed = executor.awaitTermination(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;

        assertTrue(completed, "Executor should complete within 30 seconds");
        assertTrue(errors.isEmpty(), "No errors should occur in threads: " + errors);
        assertTrue(totalTime < CONCURRENT_LIMIT_MS,
                "Should create 100 URLs concurrently in under " + CONCURRENT_LIMIT_MS + "ms, but took: " + totalTime + "ms");

        log.info("Created {} URLs concurrently in {}ms", threadCount * urlsPerThread, totalTime);
    }

    /**
     * Records 1000 clicks and verifies every one is persisted (atomic updates, no lost clicks).
     */
    @Test
    void testClickRecordingPerformance() {
        log.info("Starting testClickRecordingPerformance");

        ShortUrl url = shortUrlService.createShortUrl("https://clicks.com", 30);
        String shortCode = url.getCode();
        assertNotNull(shortCode, "Short code should not be null");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            shortUrlService.recordClick(shortCode);
        }
        long totalTime = System.currentTimeMillis() - startTime;

        log.info("Recorded 1000 clicks in {}ms", totalTime);
        assertTrue(totalTime < CLICK_LIMIT_MS,
                "Should record 1000 clicks in under " + CLICK_LIMIT_MS + "ms, but took: " + totalTime + "ms");

        long clickCount = shortUrlService.getClickCount(shortCode);
        assertEquals(1000L, clickCount,
                "Click count should be exactly 1000 after recording 1000 clicks. Got: " + clickCount);
    }
}