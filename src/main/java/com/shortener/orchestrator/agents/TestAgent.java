package com.shortener.orchestrator.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent that proposes test strategy.
 * Outputs test plan with coverage targets and test cases.
 *
 * MOCK: Does not call Claude API. Returns hardcoded response for demo.
 */
@Slf4j
@Component
public class TestAgent {

    /**
     * Generate test strategy.
     */
    public TestOutput generateStrategy(String requirements) {
        TestOutput output = new TestOutput();

        // Mock: Return hardcoded response
        output.coverageTarget = 80.0;

        output.unitTests = Arrays.asList(
                "UrlValidator: 6 tests (valid URL, SSRF blocked, edge cases)",
                "ShortUrl Entity: 4 tests (validation, expiration logic)",
                "ShortUrlService: 5 tests (create, retrieve, expiration, error handling)",
                "UrlGenerator: 3 tests (uniqueness, format, collision handling)"
        );

        output.integrationTests = Arrays.asList(
                "ShortUrlRepository with H2: 4 tests",
                "ShortUrlService + Repository: 6 tests",
                "REST API endpoints: 5 tests (create, get, errors, validation)"
        );

        output.endToEndTests = Arrays.asList(
                "Create URL → Retrieve → Validate: 1 test",
                "Expiration flow: 1 test",
                "SSRF blocking: 1 test"
        );

        output.testFrameworks = Arrays.asList(
                "JUnit 5: Test runner",
                "Mockito: Mocking dependencies",
                "Spring Test: Integration testing",
                "RestAssured: REST API testing"
        );

        output.testData = Arrays.asList(
                "Valid public URLs: example.com, google.com, etc.",
                "Invalid URLs: malformed, missing protocol",
                "SSRF payloads: localhost, 192.168.x.x, AWS metadata",
                "Edge cases: very long URLs, special characters, international URLs"
        );

        return output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestOutput {
        private double coverageTarget;
        private List<String> unitTests;
        private List<String> integrationTests;
        private List<String> endToEndTests;
        private List<String> testFrameworks;
        private List<String> testData;
    }
}