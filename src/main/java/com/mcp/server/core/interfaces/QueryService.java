package com.mcp.server.core.interfaces;

import java.util.List;
import java.util.Map;

/**
 * Service for querying the knowledge base.
 *
 * Follows Interface Segregation Principle - focused only on query operations.
 * Implementations provide search capabilities (vector, keyword, hybrid).
 */
public interface QueryService {

    /**
     * Query the knowledge base and return relevant documents.
     *
     * @param query The search query
     * @param topK Number of results to return
     * @param useHybrid Whether to use hybrid search (BM25 + Vector)
     * @param filterType Optional document type filter ('personal_note', 'technical_doc')
     * @return List of search results with content, metadata, and confidence scores
     */
    List<Map<String, Object>> query(String query, int topK, boolean useHybrid, String filterType);
}
