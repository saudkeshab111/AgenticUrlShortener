package com.shortener.orchestrator;

import com.shortener.orchestrator.agents.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for all agent outputs.
 * Verifies agents produce non-null outputs in expected format.
 */
class AgentTests {

    @Test
    void testRequirementAgentProducesOutput() {
        RequirementAgent agent = new RequirementAgent();
        RequirementAgent.RequirementOutput output = agent.analyze("Add expiration");

        assertNotNull(output.getNormalizedRequirement());
        assertFalse(output.getAcceptanceCriteria().isEmpty());
        assertFalse(output.getTasks().isEmpty());
        assertTrue(output.getTasks().size() >= 5);
    }

    @Test
    void testArchitectureAgentProducesOutput() {
        ArchitectureAgent agent = new ArchitectureAgent();
        ArchitectureAgent.ArchitectureOutput output = agent.propose("URL shortening");

        assertNotNull(output.getLayers());
        assertFalse(output.getComponents().isEmpty());
        assertFalse(output.getDataModel().isEmpty());
        assertTrue(output.getRisks().size() >= 2);
    }

    @Test
    void testSecurityAgentProducesOutput() {
        SecurityAgent agent = new SecurityAgent();
        SecurityAgent.SecurityOutput output = agent.validate("Architecture");

        assertFalse(output.getThreats().isEmpty());
        assertFalse(output.getMitigations().isEmpty());
        assertEquals(output.getThreats().size(), output.getMitigations().size());
    }

    @Test
    void testImplementationAgentProducesOutput() {
        ImplementationAgent agent = new ImplementationAgent();
        ImplementationAgent.ImplementationOutput output = agent.plan("Architecture");

        assertFalse(output.getPhases().isEmpty());
        assertFalse(output.getDependencies().isEmpty());
        assertTrue(output.getEstimatedHours() > 0);
    }

    @Test
    void testTestAgentProducesOutput() {
        TestAgent agent = new TestAgent();
        TestAgent.TestOutput output = agent.generateStrategy("Requirements");

        assertEquals(80.0, output.getCoverageTarget());
        assertFalse(output.getUnitTests().isEmpty());
        assertFalse(output.getTestFrameworks().isEmpty());
        assertTrue(output.getTestData().size() >= 3);
    }
}