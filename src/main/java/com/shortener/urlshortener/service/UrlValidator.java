package com.shortener.urlshortener.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * UrlValidator
 *
 * Validates URLs for security and format.
 * Prevents SSRF (Server-Side Request Forgery) attacks by blocking:
 * - Localhost URLs
 * - Private IP addresses (10.x, 192.168.x, 172.16-31.x)
 * - AWS metadata endpoints
 * - File system URLs
 *
 * @author Agentic SDLC Project
 * @version 1.0
 */
@Slf4j
@Component
public class UrlValidator {

    private static final List<String> BLOCKED_HOSTS = Arrays.asList(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "169.254.169.254",  // AWS metadata
            "::1",               // IPv6 localhost
            "fc00::",            // IPv6 private
            "fe80::"             // IPv6 link-local
    );

    private static final List<String> BLOCKED_PATTERNS = Arrays.asList(
            "^10\\..*",                    // 10.0.0.0/8
            "^192\\.168\\..*",             // 192.168.0.0/16
            "^172\\.(1[6-9]|2[0-9]|3[01])\\..+",  // 172.16.0.0/12
            "^127\\..*",                   // 127.0.0.0/8
            "^0\\..*"                      // 0.0.0.0/8
    );

    /**
     * Check if a URL is valid (returns boolean for tests)
     *
     * @param url the URL to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            log.debug("URL is null or empty");
            return false;
        }

        try {
            validateUrl(url);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("URL validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate a URL for security and format
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if URL is invalid or blocked
     */
    public void validateUrl(String url) {

        log.debug("Validating URL: {}", url);

        // Check if URL is empty
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        // Parse URL
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.warn("Invalid URL format: {}", url);
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }

        // Check scheme
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            log.warn("Invalid URL scheme: {}", scheme);
            throw new IllegalArgumentException("URL must use HTTP or HTTPS");
        }

        // Check host
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            log.warn("URL missing host: {}", url);
            throw new IllegalArgumentException("URL must have a host");
        }

        // Check if host is blocked
        if (isBlockedHost(host)) {
            log.warn("Blocked host detected: {} (SSRF protection)", host);
            throw new IllegalArgumentException("URL points to blocked/private network: " + host);
        }

        log.debug("URL validation passed: {}", url);
    }

    /**
     * Check if a host is in the blocked list
     *
     * @param host the hostname to check
     * @return true if host is blocked, false otherwise
     */
    private boolean isBlockedHost(String host) {

        // Check exact matches
        if (BLOCKED_HOSTS.contains(host.toLowerCase())) {
            return true;
        }

        // Check patterns (private IPs)
        for (String pattern : BLOCKED_PATTERNS) {
            if (host.matches(pattern)) {
                return true;
            }
        }

        return false;
    }
}