package com.shortener.urlshortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * Validates URLs to prevent SSRF attacks.
 * Blocks requests to private/internal networks.
 */
public class UrlValidator {

    private static final List<String> BLOCKED_HOSTS = Arrays.asList(
            "localhost", "127.0.0.1", "0.0.0.0",
            "169.254.169.254", // AWS metadata endpoint
            "10.", "172.16.", "192.168.", // Private networks
            "0.0.0.0/8", "127.0.0.0/8", "169.254.0.0/16", "172.16.0.0/12", "192.168.0.0/16"
    );

    /**
     * Validate URL: must be valid, accessible, and not point to private networks.
     */
    public static void validate(String urlString) throws IllegalArgumentException {
        if (urlString == null || urlString.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        // Check protocol
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }

        // Parse and validate
        try {
            URI uri = new URI(urlString);
            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Invalid URL: no host");
            }

            // Check for SSRF attacks
            if (isPrivateOrBlockedHost(host)) {
                throw new IllegalArgumentException("URL points to blocked/private network: " + host);
            }

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
    }

    private static boolean isPrivateOrBlockedHost(String host) {
        // Check exact matches
        if (BLOCKED_HOSTS.contains(host)) {
            return true;
        }

        // Check prefixes (for 10.x, 172.16.x, 192.168.x)
        for (String blocked : BLOCKED_HOSTS) {
            if (blocked.endsWith(".") && host.startsWith(blocked)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate a random short code.
     */
    public static String generateShortCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }
}