package com.mcp.server.retrieval;

import dev.langchain4j.data.document.Document;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Result from vector similarity search.
 *
 * Contains the document and its distance score.
 */
@Data
@AllArgsConstructor
public class VectorSearchResult {

    /**
     * The retrieved document.
     */
    private Document document;

    /**
     * Distance score (lower is better).
     * Typically cosine distance or L2 distance.
     */
    private float distance;
}
