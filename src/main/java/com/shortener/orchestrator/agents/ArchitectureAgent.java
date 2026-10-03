package com.shortener.orchestrator.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Agent that proposes system architecture and design.
 * Outputs architectural decisions and component designs.
 *
 * MOCK: Does not call Claude API. Returns hardcoded response for demo.
 */
@Slf4j
@Component
public class ArchitectureAgent {

    /**
     * Propose architecture based on requirements.
     */
    public ArchitectureOutput propose(String requirementSummary) {
        ArchitectureOutput output = new ArchitectureOutput();

        // Mock: Return hardcoded response
        output.layers = Arrays.asList(
                "Presentation Layer: REST API endpoints",
                "Service Layer: Business logic (ShortUrlService)",
                "Data Access Layer: JPA repositories",
                "Database Layer: H2 in-memory"
        );

        output.components = Arrays.asList(
                "ShortUrlController: Handles HTTP requests",
                "ShortUrlService: Orchestrates business logic",
                "UrlValidator: SSRF protection",
                "ShortUrlRepository: Database access",
                "ShortUrl Entity: Data model"
        );

        output.dataModel = Map.of(
                "ShortUrl", Arrays.asList(
                        "id (Long)",
                        "code (String, 6 char, unique)",
                        "originalUrl (TEXT)",
                        "createdAt (Instant)",
                        "expiresAt (Instant, nullable)",
                        "clickCount (Long)"
                )
        );

        output.patterns = Arrays.asList(
                "MVC pattern for REST API",
                "Repository pattern for data access",
                "Validator pattern for URL validation",
                "Strategy pattern for expiration handling"
        );

        output.risks = Arrays.asList(
                "SSRF attacks on URL shortening",
                "Storage bloat if no expiration enforced",
                "Performance degradation with large URL table"
        );

        return output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchitectureOutput {
        private List<String> layers;
        private List<String> components;
        private Map<String, List<String>> dataModel;
        private List<String> patterns;
        private List<String> risks;
    }
}