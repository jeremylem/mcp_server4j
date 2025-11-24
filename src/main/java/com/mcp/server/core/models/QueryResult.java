package com.mcp.server.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result from a knowledge base query.
 * Contains the retrieved content, metadata, and confidence score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    /**
     * The document content/text.
     */
    private String content;

    /**
     * Document metadata (source, filename, type, chunk_id, etc.).
     */
    private Map<String, String> metadata;

    /**
     * Confidence score (0.0 to 1.0).
     * Higher score indicates higher relevance.
     */
    private double confidence;
}
