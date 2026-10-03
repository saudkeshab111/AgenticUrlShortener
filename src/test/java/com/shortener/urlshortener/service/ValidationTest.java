package com.shortener.urlshortener.service;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j  // ← ADD: Enable logging
@SpringBootTest
class ValidationTest {

    @Autowired
    private ShortUrlService shortUrlService;

    @Autowired
    private UrlValidator urlValidator;

    @Autowired
    private ShortUrlRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        log.info("ValidationTest setup completed - repository cleared");
    }

    /**
     * Test that valid URLs are accepted by the validator
     */
    @Test
    void testValidUrlAccepted() {
        log.info("Starting testValidUrlAccepted");

        assertTrue(urlValidator.isValid("https://example.com"),
                "Simple HTTPS URL should be valid");
        assertTrue(urlValidator.isValid("https://www.google.com/search?q=test"),
                "URL with query parameters should be valid");
        assertTrue(urlValidator.isValid("https://github.com/user/repo"),
                "URL with path should be valid");

        log.info("✅ testValidUrlAccepted PASSED");
    }

    /**
     * Test that invalid URLs are rejected by the validator
     */
    @Test
    void testInvalidUrlRejected() {
        log.info("Starting testInvalidUrlRejected");

        assertFalse(urlValidator.isValid("not-a-url"),
                "Plain text should be invalid");
        assertFalse(urlValidator.isValid(""),
                "Empty string should be invalid");
        assertFalse(urlValidator.isValid("   "),
                "Whitespace-only string should be invalid");
        assertFalse(urlValidator.isValid("http://"),
                "Incomplete URL should be invalid");

        log.info("✅ testInvalidUrlRejected PASSED");
    }

    /**
     * Test that short codes are generated uniquely
     */
    @Test
    void testShortCodeGeneration() {
        log.info("Starting testShortCodeGeneration");

        ShortUrl url1 = shortUrlService.createShortUrl("https://example1.com", 30);
        ShortUrl url2 = shortUrlService.createShortUrl("https://example2.com", 30);

        assertNotNull(url1.getCode(), "First URL code should not be null");
        assertNotNull(url2.getCode(), "Second URL code should not be null");
        assertNotEquals(url1.getCode(), url2.getCode(),
                "Generated codes should be unique. Got: " + url1.getCode() + " and " + url2.getCode());
        assertTrue(url1.getCode().length() <= 10,
                "Code should be 10 characters or less, got: " + url1.getCode().length());
        assertTrue(url2.getCode().length() <= 10,
                "Code should be 10 characters or less, got: " + url2.getCode().length());

        log.info("Generated codes: {} and {}", url1.getCode(), url2.getCode());
        log.info("✅ testShortCodeGeneration PASSED");
    }

    /**
     * Test that expiration dates are calculated correctly
     * FIX #3: Verify 7-day expiration is exactly 7 days (not 6)
     */
    @Test
    void testExpirationDateCalculation() {
        log.info("Starting testExpirationDateCalculation");

        ShortUrl url = shortUrlService.createShortUrl("https://example.com", 7);

        assertNotNull(url.getExpiresAt(),
                "Expiration date should not be null");
        assertTrue(url.getExpiresAt().isAfter(url.getCreatedAt()),
                "Expiration should be after creation date");

        // FIX #3: Calculate days between creation and expiration
        long daysDifference = ChronoUnit.DAYS.between(url.getCreatedAt(), url.getExpiresAt());

        log.info("Created: {}, Expires: {}", url.getCreatedAt(), url.getExpiresAt());
        log.info("Days difference: {}", daysDifference);

        // Critical assertion: should be exactly 7 days, not 6
        assertEquals(7, daysDifference,
                "Expiration should be exactly 7 days after creation. Got: " + daysDifference);

        log.info("✅ testExpirationDateCalculation PASSED");
    }

    /**
     * Test that zero expiration days defaults to 1 year
     */
    @Test
    void testExpirationWithZeroDays() {
        log.info("Starting testExpirationWithZeroDays");

        ShortUrl url = shortUrlService.createShortUrl("https://example.com", 0);

        assertNotNull(url.getExpiresAt(),
                "Expiration date should not be null for zero-day input");
        assertTrue(url.getExpiresAt().isAfter(url.getCreatedAt()),
                "Expiration should be after creation date");

        long daysDifference = ChronoUnit.DAYS.between(url.getCreatedAt(), url.getExpiresAt());
        log.info("Days difference for zero-day input: {}", daysDifference);
        assertTrue(daysDifference >= 365,
                "Zero expiration days should default to ~365 days, got: " + daysDifference);

        log.info("✅ testExpirationWithZeroDays PASSED");
    }

    /**
     * Test that click count starts at zero
     */
    @Test
    void testClickCountInitialization() {
        log.info("Starting testClickCountInitialization");

        ShortUrl url = shortUrlService.createShortUrl("https://example.com", 30);

        assertNotNull(url.getClickCount(),
                "Click count should not be null");
        assertEquals(0L, url.getClickCount(),
                "Initial click count should be 0, got: " + url.getClickCount());

        log.info("URL created with click count: {}", url.getClickCount());
        log.info("✅ testClickCountInitialization PASSED");
    }

    /**
     * Test that click count increments correctly
     */
    @Test
    void testClickCountIncrement() throws InterruptedException {
        log.info("Starting testClickCountIncrement");

        ShortUrl url = shortUrlService.createShortUrl("https://example.com", 30);
        String code = url.getCode();

        log.info("Created URL with code: {}", code);

        // Record one click
        shortUrlService.recordClick(code);

        // Wait for persistence to complete
        Thread.sleep(100);

        // Retrieve and verify
        ShortUrl updated = repository.findByCode(code)
                .orElseThrow(() -> new AssertionError("URL not found after click: " + code));

        log.info("Click recorded. Updated click count: {}", updated.getClickCount());
        assertEquals(1L, updated.getClickCount(),
                "Click count should be 1 after one click, got: " + updated.getClickCount());

        log.info("✅ testClickCountIncrement PASSED");
    }

    /**
     * Test that multiple clicks are recorded correctly
     */
    @Test
    void testMultipleClickRecording() throws InterruptedException {
        log.info("Starting testMultipleClickRecording");

        ShortUrl url = shortUrlService.createShortUrl("https://example.com", 30);
        String code = url.getCode();

        // Record 5 clicks
        for (int i = 0; i < 5; i++) {
            shortUrlService.recordClick(code);
            log.debug("Recorded click {}", i + 1);
        }

        // Wait for persistence
        Thread.sleep(100);

        // Retrieve and verify
        ShortUrl updated = repository.findByCode(code)
                .orElseThrow(() -> new AssertionError("URL not found after clicks: " + code));

        log.info("Recorded 5 clicks. Updated click count: {}", updated.getClickCount());
        assertEquals(5L, updated.getClickCount(),
                "Click count should be 5 after 5 clicks, got: " + updated.getClickCount());

        log.info("✅ testMultipleClickRecording PASSED");
    }

    /**
     * Test that duplicate URLs get different short codes
     */
    @Test
    void testDuplicateUrlCreation() {
        log.info("Starting testDuplicateUrlCreation");

        String url = "https://example.com";

        ShortUrl shortUrl1 = shortUrlService.createShortUrl(url, 30);
        ShortUrl shortUrl2 = shortUrlService.createShortUrl(url, 30);

        assertNotNull(shortUrl1.getCode(),
                "First short code should not be null");
        assertNotNull(shortUrl2.getCode(),
                "Second short code should not be null");
        assertNotEquals(shortUrl1.getCode(), shortUrl2.getCode(),
                "Duplicate URLs should get different codes. Got: " + shortUrl1.getCode() + " and " + shortUrl2.getCode());

        log.info("Same URL generated different codes: {} and {}",
                shortUrl1.getCode(), shortUrl2.getCode());
        log.info("✅ testDuplicateUrlCreation PASSED");
    }

    /**
     * Test that data persists correctly to database
     */
    @Test
    void testDataPersistence() {
        log.info("Starting testDataPersistence");

        String originalUrl = "https://persistence.com";
        ShortUrl created = shortUrlService.createShortUrl(originalUrl, 30);

        log.info("Created URL with code: {}", created.getCode());

        ShortUrl retrieved = repository.findByCode(created.getCode())
                .orElseThrow(() -> new AssertionError("URL not found in database: " + created.getCode()));

        assertEquals(created.getOriginalUrl(), retrieved.getOriginalUrl(),
                "Original URL should match. Expected: " + created.getOriginalUrl() +
                        ", Got: " + retrieved.getOriginalUrl());
        assertEquals(created.getCode(), retrieved.getCode(),
                "Code should match. Expected: " + created.getCode() +
                        ", Got: " + retrieved.getCode());

        log.info("Data persisted correctly: {} -> {}", retrieved.getCode(), retrieved.getOriginalUrl());
        log.info("✅ testDataPersistence PASSED");
    }

    /**
     * Test that timestamps are managed correctly
     */
    @Test
    void testTimestampManagement() {
        log.info("Starting testTimestampManagement");

        Instant beforeCreation = Instant.now();
        ShortUrl url = shortUrlService.createShortUrl("https://timestamp.com", 30);
        Instant afterCreation = Instant.now();

        assertNotNull(url.getCreatedAt(),
                "Created timestamp should not be null");
        assertTrue(url.getCreatedAt().isAfter(beforeCreation.minus(1, ChronoUnit.SECONDS)),
                "Created timestamp should be near current time");
        assertTrue(url.getCreatedAt().isBefore(afterCreation.plus(1, ChronoUnit.SECONDS)),
                "Created timestamp should be near current time");

        log.info("URL created at: {}", url.getCreatedAt());
        log.info("Current time range: {} to {}", beforeCreation, afterCreation);
        log.info("✅ testTimestampManagement PASSED");
    }
}