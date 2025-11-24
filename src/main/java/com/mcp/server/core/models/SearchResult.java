package com.mcp.server.core.models;

/**
 * Represents a search result from keyword indexing.
 * Renamed from BM25SearchResult to be more generic and allow
 * different keyword indexing implementations.
 */
public record SearchResult(String id, String content, String filename, float score) {

    /**
     * Creates a search result.
     *
     * @param id       Document ID
     * @param content  Document content
     * @param filename Source filename
     * @param score    Relevance score
     */
    public SearchResult {
    }

    @Override
    public String toString() {
        return String.format("SearchResult{id='%s', filename='%s', score=%.4f, content='%s'}",
                id, filename, score, content.substring(0, Math.min(50, content.length())) + "...");
    }
}
