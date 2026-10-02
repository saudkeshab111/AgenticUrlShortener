package com.shortener.urlshortener.service;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service layer for URL shortening operations.
 * Handles validation, creation, and retrieval of shortened URLs.
 */
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlRepository repository;
    private static final int MAX_RETRY = 5;

    /**
     * Create a new shortened URL.
     *
     * @param originalUrl The URL to shorten
     * @param expirationDays Number of days until expiration (0 = never)
     * @return The created ShortUrl
     */
    @Transactional
    public ShortUrl create(String originalUrl, int expirationDays) {
        // Validate URL (SSRF protection)
        UrlValidator.validate(originalUrl);

        // Generate unique short code
        String code = generateUniqueCode();

        // Create entity
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setCode(code);
        shortUrl.setOriginalUrl(originalUrl);
        shortUrl.setCreatedAt(Instant.now());
        shortUrl.setClickCount(0L);

        // Set expiration if requested
        if (expirationDays > 0) {
            shortUrl.setExpiresAt(Instant.now().plus(expirationDays, ChronoUnit.DAYS));
        }

        return repository.save(shortUrl);
    }

    /**
     * Retrieve a URL by its short code.
     * Returns empty Optional if URL is expired.
     */
    @Transactional
    public Optional<ShortUrl> getByCode(String code) {
        Optional<ShortUrl> url = repository.findByCode(code);

        if (url.isPresent()) {
            ShortUrl shortUrl = url.get();

            // Check expiration
            if (shortUrl.hasExpired()) {
                return Optional.empty();
            }

            // Record click
            shortUrl.recordClick();
            repository.save(shortUrl);

            return Optional.of(shortUrl);
        }

        return Optional.empty();
    }

    /**
     * Delete a shortened URL.
     */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Generate a unique short code (retry if collision).
     */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            String code = UrlValidator.generateShortCode();
            if (!repository.existsByCode(code)) {
                return code;
            }
        }
        throw new RuntimeException("Could not generate unique code after " + MAX_RETRY + " attempts");
    }
}