package com.shortener.orchestrator.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent that plans implementation tasks.
 * Outputs a detailed implementation plan with phases.
 *
 * MOCK: Does not call Claude API. Returns hardcoded response for demo.
 */

@Component
public class ImplementationAgent {

    /**
     * Create implementation plan.
     */
    public ImplementationOutput plan(String architecture) {
        ImplementationOutput output = new ImplementationOutput();

        // Mock: Return hardcoded response
        output.phases = Arrays.asList(
                new Phase("Phase 1: Model Layer", 1, Arrays.asList(
                        "Create ShortUrl JPA entity",
                        "Add validation annotations",
                        "Create ShortUrlRepository interface"
                )),
                new Phase("Phase 2: Service Layer", 2, Arrays.asList(
                        "Implement ShortUrlService",
                        "Add SSRF validation logic",
                        "Add URL generation and uniqueness check",
                        "Add expiration handling"
                )),
                new Phase("Phase 3: API Layer", 2, Arrays.asList(
                        "Create ShortUrlController",
                        "Implement POST /api/v1/urls endpoint",
                        "Implement GET /api/v1/{code} endpoint",
                        "Add error handling and validation"
                )),
                new Phase("Phase 4: Configuration", 1, Arrays.asList(
                        "Configure H2 database",
                        "Set up JPA mappings",
                        "Configure Spring Data JPA"
                )),
                new Phase("Phase 5: Testing", 2, Arrays.asList(
                        "Unit tests for UrlValidator",
                        "Integration tests for ShortUrlService",
                        "Controller tests for REST API",
                        "End-to-end tests"
                ))
        );

        output.dependencies = Arrays.asList(
                "Spring Boot Starter Web",
                "Spring Boot Starter Data JPA",
                "H2 Database",
                "Lombok",
                "JUnit 5",
                "Mockito"
        );

        output.estimatedHours = 8;
        output.riskAreas = Arrays.asList(
                "SSRF validation complexity",
                "Concurrent code generation",
                "H2 schema management in tests"
        );

        return output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Phase {
        private String name;
        private int estimatedDays;
        private List<String> tasks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImplementationOutput {
        private List<Phase> phases;
        private List<String> dependencies;
        private int estimatedHours;
        private List<String> riskAreas;
    }
}