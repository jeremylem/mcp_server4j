package com.mcp.server.mcp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoint for Claude Code compatibility.
 * Matches the Python implementation's /health endpoint.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "healthy",
                "service", "mcp_knowledge_base_baseline_java"
        );
    }
}
