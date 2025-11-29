package com.mcp.server.retrieval;

import dev.langchain4j.data.document.Document;

/**
 * Result from vector similarity search.
 * Distance score: lower is better (cosine or L2 distance).
 */
public record VectorSearchResult(Document document, float distance) {
}
