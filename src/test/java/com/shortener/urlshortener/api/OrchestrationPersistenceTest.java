package com.shortener.urlshortener.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrchestrationPersistenceTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testExecutionTracePersistedOnSuccess() throws Exception {
        String requestBody = "{\"originalUrl\": \"https://example.com\", \"expirationDays\": 30}";

        mockMvc.perform(post("/api/v1/urls/orchestrated")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.workflowId").exists())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.executionTimeMs").exists());
    }

    @Test
    void testExecutionTraceRetrievedByWorkflowId() throws Exception {
        // First, create a workflow
        String requestBody = "{\"originalUrl\": \"https://github.com\", \"expirationDays\": 7}";

        String response = mockMvc.perform(post("/api/v1/urls/orchestrated")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract workflowId from response
        String workflowId = response.contains("\"workflowId\":\"")
                ? response.split("\"workflowId\":\"")[1].split("\"")[0]
                : "test-id";

        // Then retrieve the trace
        mockMvc.perform(get("/api/v1/orchestration/traces/" + workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value(workflowId))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testTraceNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/orchestration/traces/non-existent-workflow"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void testInvalidUrlReturns400() throws Exception {
        String requestBody = "{\"originalUrl\": \"\", \"expirationDays\": 30}";

        mockMvc.perform(post("/api/v1/urls/orchestrated")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}