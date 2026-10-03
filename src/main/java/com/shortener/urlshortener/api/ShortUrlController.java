package com.shortener.urlshortener.api;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.service.ShortUrlService;
import com.shortener.urlshortener.service.ShortUrlService.ShortUrlExpiredException;
import com.shortener.urlshortener.service.ShortUrlService.ShortUrlNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    /**
     * POST /api/v1/urls - create a short URL.
     * expirationDays: optional, 0 or missing = service default (365 days).
     */
    @PostMapping("/api/v1/urls")
    public ResponseEntity<Map<String, Object>> createShortUrl(@RequestBody Map<String, Object> request) {
        try {
            String originalUrl = parseOriginalUrl(request.get("originalUrl"));
            int expirationDays = parseExpirationDays(request.get("expirationDays"));

            ShortUrl shortUrl = shortUrlService.createShortUrl(originalUrl, expirationDays);

            Map<String, Object> response = new HashMap<>();
            response.put("code", shortUrl.getCode());
            response.put("shortUrl", buildShortLink(shortUrl.getCode()));
            response.put("originalUrl", shortUrl.getOriginalUrl());
            response.put("createdAt", shortUrl.getCreatedAt());
            response.put("expiresAt", shortUrl.getExpiresAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        }
    }

    /**
     * GET /api/v1/{code} - JSON lookup. Counts as a click.
     */
    @GetMapping("/api/v1/{code}")
    public ResponseEntity<Map<String, Object>> getShortUrl(@PathVariable String code) {
        try {
            ShortUrl shortUrl = shortUrlService.resolveAndRecordClick(code);

            Map<String, Object> response = new HashMap<>();
            response.put("code", shortUrl.getCode());
            response.put("originalUrl", shortUrl.getOriginalUrl());
            response.put("clickCount", shortUrl.getClickCount());
            response.put("createdAt", shortUrl.getCreatedAt());
            response.put("expiresAt", shortUrl.getExpiresAt());

            return ResponseEntity.ok(response);

        } catch (ShortUrlNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, "URL not found", e.getMessage());
        } catch (ShortUrlExpiredException e) {
            return error(HttpStatus.NOT_FOUND, "URL expired", e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        }
    }

    /**
     * GET /s/{code} - the actual short link. Redirects (302) to the original URL and counts a click.
     */
    @GetMapping("/s/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        try {
            ShortUrl shortUrl = shortUrlService.resolveAndRecordClick(code);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(shortUrl.getOriginalUrl()))
                    .build();
        } catch (ShortUrlNotFoundException | ShortUrlExpiredException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String buildShortLink(String code) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/s/{code}")
                .buildAndExpand(code)
                .toUriString();
    }

    private static String parseOriginalUrl(Object value) {
        if (!(value instanceof String url) || url.isBlank()) {
            throw new IllegalArgumentException("originalUrl is required");
        }
        return url;
    }

    private static int parseExpirationDays(Object value) {
        if (value == null) {
            return 0;
        }
        int days;
        if (value instanceof Number number) {
            days = number.intValue();
        } else if (value instanceof String text && !text.isBlank()) {
            try {
                days = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("expirationDays must be a whole number");
            }
        } else {
            throw new IllegalArgumentException("expirationDays must be a whole number");
        }
        if (days < 0) {
            throw new IllegalArgumentException("expirationDays cannot be negative");
        }
        return days;
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", Objects.toString(message, error));
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }
}