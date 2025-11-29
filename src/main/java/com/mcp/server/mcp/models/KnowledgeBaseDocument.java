package com.mcp.server.mcp.models;

import java.util.Map;

/**
 * Document returned from knowledge base query.
 * Contains text content, metadata, and hybrid search confidence score (0.0-1.0).
 */
public record KnowledgeBaseDocument(
        String content,
        Map<String, String> metadata,
        double confidence
) {
}
