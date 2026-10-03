package com.shortener.urlshortener.service;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ShortUrlService {

    /**
     * Used when expirationDays is 0 or less.
     * Assumption: short URLs always expire, so storage cannot grow forever.
     */
    public static final int DEFAULT_EXPIRATION_DAYS = 365;

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final ShortUrlRepository repository;
    private final UrlValidator validator;

    public ShortUrlService(ShortUrlRepository repository, UrlValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /**
     * Create a shortened URL.
     */
    public ShortUrl createShortUrl(String originalUrl, int expirationDays) {
        if (!validator.isValid(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL: " + originalUrl);
        }

        String code = generateUniqueCode();
        Instant now = Instant.now();
        int days = expirationDays > 0 ? expirationDays : DEFAULT_EXPIRATION_DAYS;

        ShortUrl shortUrl = ShortUrl.builder()
                .code(code)
                .originalUrl(originalUrl)
                .createdAt(now)
                .expiresAt(now.plus(days, ChronoUnit.DAYS))
                .clickCount(0L)
                .build();

        ShortUrl saved = repository.save(shortUrl);
        log.info("Created short URL: {} -> {}", code, originalUrl);
        return saved;
    }

    /**
     * Look up a short URL by code (read-only, does not count a click, ignores expiry).
     */
    @Transactional(readOnly = true)
    public ShortUrl getShortUrl(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Short URL code cannot be null or empty");
        }
        return repository.findByCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code));
    }

    /**
     * Resolve a short URL for a visitor: rejects expired URLs, counts the click,
     * and returns the URL with the updated click count. All in one transaction.
     */
    public ShortUrl resolveAndRecordClick(String code) {
        ShortUrl shortUrl = getShortUrl(code);

        if (shortUrl.hasExpired()) {
            throw new ShortUrlExpiredException(code);
        }

        repository.incrementClickCount(code);

        // Re-read so the returned object has the new click count
        return getShortUrl(code);
    }

    /**
     * Record a click using a single atomic UPDATE (safe under concurrency).
     */
    public void recordClick(String code) {
        if (code == null || code.isBlank()) {
            log.warn("Attempted to record click with null or empty code");
            return;
        }

        int updated = repository.incrementClickCount(code);
        if (updated == 0) {
            throw new ShortUrlNotFoundException(code);
        }
        log.debug("Recorded click for: {}", code);
    }

    /**
     * Generate a short code that is not already in use.
     * Retries on collision instead of failing on the database unique constraint.
     */
    public String generateUniqueCode() {
        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 8).toUpperCase();
            if (repository.findByCode(code).isEmpty()) {
                return code;
            }
            log.warn("Short code collision on attempt {}: {}", attempt, code);
        }
        throw new IllegalStateException(
                "Could not generate a unique short code after " + MAX_CODE_ATTEMPTS + " attempts");
    }

    @Transactional(readOnly = true)
    public boolean exists(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return repository.findByCode(code).isPresent();
    }

    public void deleteShortUrl(String code) {
        repository.findByCode(code).ifPresent(repository::delete);
        log.info("Deleted short URL: {}", code);
    }

    @Transactional(readOnly = true)
    public Long getClickCount(String code) {
        return getShortUrl(code).getClickCount();
    }

    /** Thrown when a code does not exist. Extends IllegalArgumentException for backward compatibility. */
    public static class ShortUrlNotFoundException extends IllegalArgumentException {
        public ShortUrlNotFoundException(String code) {
            super("Short URL not found: " + code);
        }
    }

    /** Thrown when a code exists but has passed its expiration time. */
    public static class ShortUrlExpiredException extends RuntimeException {
        public ShortUrlExpiredException(String code) {
            super("Short URL has expired: " + code);
        }
    }
}