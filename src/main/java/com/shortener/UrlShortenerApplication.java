package com.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Agentic URL Shortener application.
 *
 * This application demonstrates an orchestration-first approach to SDLC,
 * where agents coordinate through dependency graphs with gates, policies,
 * and human approval checkpoints.
 *
 * See: /docs/orchestrator-design.md
 */
@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}