package com.shortener.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a shortened URL.
 * Tracks original URL, code, creation time, expiration, and click count.
 */
@Entity
@Table(name = "short_urls")
@Data
@Builder
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

    // @Builder.Default makes the builder respect the 0L default (fixes the compiler warning)
    @Builder.Default
    @Column(nullable = false)
    private Long clickCount = 0L;

    @PrePersist
    protected void onCreate() {
        // Only fill in values that were not set, so the service's timestamp is kept
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (clickCount == null) {
            clickCount = 0L;
        }
    }

    public boolean isValid() {
        if (expiresAt == null) {
            return true;
        }
        return Instant.now().isBefore(expiresAt);
    }

    public boolean hasExpired() {
        return !isValid();
    }

    /**
     * In-memory increment. The service uses the atomic repository update instead;
     * this is kept null-safe for any code that still calls it.
     */
    public void recordClick() {
        this.clickCount = (this.clickCount == null ? 0L : this.clickCount) + 1;
    }
}