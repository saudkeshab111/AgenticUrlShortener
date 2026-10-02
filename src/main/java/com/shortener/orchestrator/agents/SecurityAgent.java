package com.shortener.orchestrator.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Agent that validates security requirements.
 * Outputs security concerns and mitigations.
 *
 * MOCK: Does not call Claude API. Returns hardcoded response for demo.
 */
public class SecurityAgent {

    /**
     * Validate security for the architecture.
     */
    public SecurityOutput validate(String architecture) {
        SecurityOutput output = new SecurityOutput();

        // Mock: Return hardcoded response
        output.threats = Arrays.asList(
                "SSRF (Server-Side Request Forgery): Attacker provides URL pointing to internal resources",
                "Information Disclosure: Error messages reveal system details",
                "DoS: Attacker creates millions of short URLs",
                "URL Hijacking: Expired URLs could be reassigned to attacker URLs"
        );

        output.mitigations = Arrays.asList(
                "SSRF: Block private network ranges (127.0.0.0/8, 192.168.0.0/16, etc.)",
                "Information Disclosure: Generic error messages without stack traces",
                "DoS: Rate limiting on URL creation endpoint",
                "URL Hijacking: Permanent reservation of deleted short codes or async cleanup"
        );

        output.controls = Arrays.asList(
                "Input validation on all URLs",
                "Whitelist approach for blocked hosts",
                "No sensitive data in logs",
                "HTTPS only (enforce in production)",
                "SQL injection prevention (use JPA parameterized queries)"
        );

        output.complianceNotes = Arrays.asList(
                "No PII stored in shortened URLs",
                "No authentication required initially",
                "Audit logging recommended for production"
        );

        return output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityOutput {
        private List<String> threats;
        private List<String> mitigations;
        private List<String> controls;
        private List<String> complianceNotes;
    }
}