package com.mcp.server.core.models;

/**
 * Represents a search result from keyword indexing.
 * Renamed from BM25SearchResult to be more generic and allow
 * different keyword indexing implementations.
 *
 * @param id       Document ID
 * @param content  Document content
 * @param filename Source filename
 * @param score    Relevance score
 */
public record SearchResult(String id, String content, String filename, float score) {
}
