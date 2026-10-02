package com.shortener.orchestrator.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Agent that parses and normalizes requirements.
 * Outputs a structured requirement document with acceptance criteria.
 *
 * MOCK: Does not call Claude API. Returns hardcoded response for demo.
 */
public class RequirementAgent {

    /**
     * Analyze a requirement and produce structured output.
     */
    public RequirementOutput analyze(String requirement) {
        RequirementOutput output = new RequirementOutput();

        // Mock: Return hardcoded response
        output.normalizedRequirement = "URL shortening with expiration support";
        output.acceptanceCriteria = Arrays.asList(
                "User can shorten any public URL",
                "Shortened code is 6 alphanumeric characters",
                "URL expiration can be set in days",
                "Expired URLs return 404"
        );
        output.assumptions = Arrays.asList(
                "URLs are publicly accessible",
                "No authentication required initially",
                "H2 in-memory database sufficient for MVP"
        );
        output.ambiguities = Arrays.asList(
                "Should we support custom short codes?",
                "What is the max URL length?",
                "Rate limiting required?"
        );
        output.tasks = Arrays.asList(
                "Create ShortUrl entity with JPA",
                "Implement POST /urls endpoint",
                "Implement GET /{code} endpoint",
                "Add SSRF validation",
                "Add expiration logic",
                "Add click tracking",
                "Create comprehensive tests"
        );

        return output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementOutput {
        private String normalizedRequirement;
        private List<String> acceptanceCriteria;
        private List<String> assumptions;
        private List<String> ambiguities;
        private List<String> tasks;
    }
}