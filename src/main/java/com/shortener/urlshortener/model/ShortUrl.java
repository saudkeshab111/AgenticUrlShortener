package com.shortener.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a shortened URL.
 * Tracks original URL, code, creation time, and expiration.
 */
@Entity
@Table(name = "short_urls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant expiresAt;

    @Column
    private Long clickCount = 0L;

    /**
     * Check if this shortened URL is still valid (not expired).
     */
    public boolean isValid() {
        if (expiresAt == null) {
            return true; // No expiration
        }
        return Instant.now().isBefore(expiresAt);
    }

    /**
     * Increment click counter.
     */
    public void recordClick() {
        this.clickCount++;
    }

    /**
     * Check if URL has expired.
     */
    public boolean hasExpired() {
        return !isValid();
    }
}