package com.shortener.urlshortener.api;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.service.ShortUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for URL shortening.
 * Handles creation and retrieval of shortened URLs.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService service;

    /**
     * POST /v1/urls
     * Create a new shortened URL.
     *
     * Request body:
     * {
     *   "originalUrl": "https://www.example.com/very/long/url",
     *   "expirationDays": 30
     * }
     */
    @PostMapping("/urls")
    public ResponseEntity<Map<String, Object>> createShortUrl(
            @RequestBody CreateUrlRequest request) {

        try {
            ShortUrl created = service.create(
                    request.getOriginalUrl(),
                    request.getExpirationDays()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("code", created.getCode());
            response.put("shortUrl", "http://localhost:8080/" + created.getCode());
            response.put("originalUrl", created.getOriginalUrl());
            response.put("createdAt", created.getCreatedAt());
            response.put("expiresAt", created.getExpiresAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException(e.getMessage());
        }
    }

    /**
     * GET /{code}
     * Retrieve and redirect to original URL.
     */
    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> getShortUrl(@PathVariable String code) {
        return service.getByCode(code)
                .map(shortUrl -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("code", shortUrl.getCode());
                    response.put("originalUrl", shortUrl.getOriginalUrl());
                    response.put("clickCount", shortUrl.getClickCount());
                    response.put("createdAt", shortUrl.getCreatedAt());
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new UrlNotFoundException("URL not found or expired: " + code));
    }

    /**
     * Request DTO for creating shortened URLs.
     */
    public static class CreateUrlRequest {
        private String originalUrl;
        private int expirationDays = 0; // 0 = no expiration

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public int getExpirationDays() {
            return expirationDays;
        }

        public void setExpirationDays(int expirationDays) {
            this.expirationDays = expirationDays;
        }
    }
}