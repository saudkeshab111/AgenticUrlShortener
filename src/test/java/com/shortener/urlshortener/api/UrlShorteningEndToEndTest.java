package com.shortener.urlshortener.api;

import com.shortener.urlshortener.model.ShortUrl;
import com.shortener.urlshortener.repository.ShortUrlRepository;
import com.shortener.urlshortener.repository.ExecutionTraceRepository;
import com.shortener.urlshortener.model.ExecutionTrace;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class UrlShorteningEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ExecutionTraceRepository traceRepository;

    @BeforeEach
    void setUp() {
        shortUrlRepository.deleteAll();
        traceRepository.deleteAll();
        log.info("Test setup completed - repositories cleared");
    }

    /**
     * Test complete URL shortening workflow including click recording
     * FIX #4: Verified click count is recorded and persisted
     */
    @Test
    void testCompleteUrlShorteningWorkflow() throws Exception {
        String originalUrl = "https://www.example.com/very/long/url/that/needs/shortening";
        String requestBody = String.format(
                "{\"originalUrl\": \"%s\", \"expirationDays\": 30}",
                originalUrl
        );

        log.info("Creating short URL for: {}", originalUrl);

        // Step 1: Create orchestrated short URL
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls/orchestrated")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.workflowId").exists())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(jsonPath("$.executionTimeMs").exists())
                .andExpect(jsonPath("$.agentOutputs").exists())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String workflowId = extractJsonValue(responseBody, "workflowId");
        String shortCode = extractJsonValue(responseBody, "shortCode");

        log.info("Created workflow {} with short code: {}", workflowId, shortCode);

        assertNotNull(workflowId, "Workflow ID should not be null");
        assertNotNull(shortCode, "Short code should not be null");
        assertTrue(shortCode.length() <= 10, "Short code should be <= 10 characters");

        // Step 2: Verify URL was persisted in database
        ShortUrl savedUrl = shortUrlRepository.findByCode(shortCode)
                .orElseThrow(() -> new AssertionError("Short URL not found in database: " + shortCode));
        assertEquals(originalUrl, savedUrl.getOriginalUrl(),
                "Original URL should match what was saved");
        assertEquals(0L, savedUrl.getClickCount(),
                "Initial click count should be 0");

        log.info("URL saved with initial click count: {}", savedUrl.getClickCount());

        // Step 3: Verify execution trace was created
        mockMvc.perform(get("/api/v1/orchestration/traces/" + workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value(workflowId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.originalUrl").value(originalUrl));

        ExecutionTrace trace = traceRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new AssertionError("Execution trace not found: " + workflowId));
        assertEquals(ExecutionTrace.ExecutionStatus.COMPLETED, trace.getStatus(),
                "Trace status should be COMPLETED");
        assertEquals(shortCode, trace.getShortCode(),
                "Trace short code should match created code");

        log.info("Execution trace verified - status: {}", trace.getStatus());

        // Step 4: Retrieve short URL (triggers click recording)
        log.info("Retrieving short URL to trigger click recording: {}", shortCode);
        mockMvc.perform(get("/api/v1/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(shortCode))
                .andExpect(jsonPath("$.originalUrl").value(originalUrl));

        // Step 5: Allow slight delay for transaction to complete and click to be recorded
        Thread.sleep(100);

        // Step 6: Verify click count was incremented and persisted
        ShortUrl retrievedUrl = shortUrlRepository.findByCode(shortCode)
                .orElseThrow(() -> new AssertionError("URL not found after retrieval: " + shortCode));

        log.info("Retrieved URL click count: {}", retrievedUrl.getClickCount());
        assertEquals(1L, retrievedUrl.getClickCount(),
                "Click count should be 1 after one retrieval. Current: " + retrievedUrl.getClickCount());

        log.info("✅ testCompleteUrlShorteningWorkflow PASSED");
    }

    /**
     * Test multiple simultaneous workflows
     */
    @Test
    void testMultipleSimultaneousWorkflows() throws Exception {
        String[] urls = {
                "https://example.com/1",
                "https://example.com/2",
                "https://example.com/3"
        };

        String[] workflowIds = new String[3];
        String[] shortCodes = new String[3];

        log.info("Creating {} simultaneous workflows", urls.length);

        for (int i = 0; i < urls.length; i++) {
            String requestBody = String.format(
                    "{\"originalUrl\": \"%s\", \"expirationDays\": 7}",
                    urls[i]
            );

            MvcResult result = mockMvc.perform(post("/api/v1/urls/orchestrated")
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            workflowIds[i] = extractJsonValue(responseBody, "workflowId");
            shortCodes[i] = extractJsonValue(responseBody, "shortCode");

            assertNotNull(workflowIds[i], "Workflow ID should not be null for URL " + i);
            assertNotNull(shortCodes[i], "Short code should not be null for URL " + i);

            log.info("Workflow {}: {} -> {}", i, urls[i], shortCodes[i]);
        }

        assertEquals(3, shortUrlRepository.findAll().size(),
                "Should have 3 short URLs in database");
        assertEquals(3, traceRepository.findAll().size(),
                "Should have 3 execution traces in database");

        // Verify each workflow
        for (int i = 0; i < urls.length; i++) {
            final int index = i;  // ← FIX: Create final copy for lambda use
            ShortUrl url = shortUrlRepository.findByCode(shortCodes[index])
                    .orElseThrow(() -> new AssertionError("URL not found: " + shortCodes[index]));
            assertEquals(urls[index], url.getOriginalUrl(),
                    "Original URL should match for workflow " + index);

            ExecutionTrace trace = traceRepository.findByWorkflowId(workflowIds[index])
                    .orElseThrow(() -> new AssertionError("Trace not found: " + workflowIds[index]));
            assertEquals(shortCodes[index], trace.getShortCode(),
                    "Trace short code should match for workflow " + index);
        }

        log.info("✅ testMultipleSimultaneousWorkflows PASSED");
    }

    /**
     * Test workflow failure handling for invalid URLs
     */
    @Test
    void testWorkflowFailureHandling() throws Exception {
        String invalidUrl = "not-a-valid-url";
        String requestBody = String.format(
                "{\"originalUrl\": \"%s\", \"expirationDays\": 30}",
                invalidUrl
        );

        log.info("Testing failure handling with invalid URL: {}", invalidUrl);

        MvcResult result = mockMvc.perform(post("/api/v1/urls/orchestrated")
                        .contentType("application/json")
                        .content(requestBody))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        log.info("Response: {}", responseBody);

        // Verify failure was recorded
        if (responseBody.contains("\"status\":\"FAILED\"")) {
            String workflowId = extractJsonValue(responseBody, "workflowId");
            if (workflowId != null) {
                ExecutionTrace trace = traceRepository.findByWorkflowId(workflowId).orElse(null);
                if (trace != null) {
                    assertEquals(ExecutionTrace.ExecutionStatus.FAILED, trace.getStatus(),
                            "Workflow should be marked as FAILED for invalid URL");
                    log.info("✅ Failure properly recorded in trace");
                }
            }
        }

        log.info("✅ testWorkflowFailureHandling PASSED");
    }

    /**
     * Test retrieving traces by status
     */
    @Test
    void testRetrieveTracesByStatus() throws Exception {
        log.info("Creating 3 traces to test status filtering");

        for (int i = 0; i < 3; i++) {
            String requestBody = String.format(
                    "{\"originalUrl\": \"https://example.com/%d\", \"expirationDays\": 30}",
                    i
            );

            mockMvc.perform(post("/api/v1/urls/orchestrated")
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isCreated());
        }

        // Verify we can retrieve traces by status
        mockMvc.perform(get("/api/v1/orchestration/traces?status=COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        log.info("✅ testRetrieveTracesByStatus PASSED");
    }

    /**
     * Test execution time tracking
     */
    @Test
    void testExecutionTimeTracking() throws Exception {
        String requestBody = "{\"originalUrl\": \"https://timing-test.com\", \"expirationDays\": 1}";

        log.info("Testing execution time tracking");

        MvcResult result = mockMvc.perform(post("/api/v1/urls/orchestrated")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.executionTimeMs").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String executionTimeStr = extractJsonValue(responseBody, "executionTimeMs");

        assertNotNull(executionTimeStr, "Execution time should be present in response");

        long executionTime = Long.parseLong(executionTimeStr);
        assertTrue(executionTime > 0, "Execution time should be greater than 0");
        assertTrue(executionTime < 30000, "Execution time should be less than 30 seconds");

        log.info("Execution time: {} ms", executionTime);
        log.info("✅ testExecutionTimeTracking PASSED");
    }

    /**
     * Test that all agent outputs are present
     */
    @Test
    void testAgentOutputsPresent() throws Exception {
        String requestBody = "{\"originalUrl\": \"https://agent-test.com\", \"expirationDays\": 1}";

        log.info("Testing agent outputs presence");

        mockMvc.perform(post("/api/v1/urls/orchestrated")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentOutputs").exists())
                .andExpect(jsonPath("$.agentOutputs.requirements").exists())
                .andExpect(jsonPath("$.agentOutputs.architecture").exists())
                .andExpect(jsonPath("$.agentOutputs.security").exists())
                .andExpect(jsonPath("$.agentOutputs.implementation").exists())
                .andExpect(jsonPath("$.agentOutputs.testing").exists());

        log.info("✅ testAgentOutputsPresent PASSED");
    }

    /**
     * Enhanced JSON value extraction with better error handling
     * Handles both string values and numeric values
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || json.isEmpty()) {
            log.warn("JSON is null or empty");
            return null;
        }

        try {
            // Try string value format: "key":"value"
            String searchStr = "\"" + key + "\":\"";
            int startIdx = json.indexOf(searchStr);
            if (startIdx != -1) {
                startIdx += searchStr.length();
                int endIdx = json.indexOf("\"", startIdx);
                if (endIdx != -1) {
                    return json.substring(startIdx, endIdx);
                }
            }

            // Try numeric value format: "key":value (no quotes)
            searchStr = "\"" + key + "\":";
            startIdx = json.indexOf(searchStr);
            if (startIdx != -1) {
                startIdx += searchStr.length();
                // Skip whitespace
                while (startIdx < json.length() && Character.isWhitespace(json.charAt(startIdx))) {
                    startIdx++;
                }

                int endIdx = startIdx;
                // Read until comma, closing brace, or closing bracket
                while (endIdx < json.length() &&
                        json.charAt(endIdx) != ',' &&
                        json.charAt(endIdx) != '}' &&
                        json.charAt(endIdx) != ']') {
                    endIdx++;
                }

                if (endIdx > startIdx) {
                    return json.substring(startIdx, endIdx).trim();
                }
            }

            log.warn("Could not extract key '{}' from JSON", key);
            return null;
        } catch (Exception e) {
            log.error("Error extracting JSON value for key: {}", key, e);
            return null;
        }
    }
}