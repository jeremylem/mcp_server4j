package com.mcp.server.mcp.models;

import java.util.Map;

/**
 * Document returned from knowledge base query.
 *
 * Represents a single result from the RAG system with:
 * - content: The text content of the document chunk
 * - metadata: Source file, type, and other metadata
 * - confidence: Hybrid search score (0.0 to 1.0)
 */
public class KnowledgeBaseDocument {

    private String content;
    private Map<String, String> metadata;
    private double confidence;

    public KnowledgeBaseDocument() {
    }

    public KnowledgeBaseDocument(String content, Map<String, String> metadata, double confidence) {
        this.content = content;
        this.metadata = metadata;
        this.confidence = confidence;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
