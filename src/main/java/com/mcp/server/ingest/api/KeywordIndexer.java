package com.mcp.server.ingest.api;

import com.mcp.server.core.models.SearchResult;
import com.mcp.server.ingest.exception.IndexingException;
import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Builds and searches a keyword-based index (e.g., BM25, TF-IDF).
 *
 * Implementations should handle index creation, query parsing, and result ranking.
 */
public interface KeywordIndexer extends AutoCloseable {

    /**
     * Build the index from documents.
     *
     * @param documents Documents to index
     * @throws IndexingException if indexing fails
     */
    void buildIndex(List<Document> documents);

    /**
     * Search the index for a query.
     *
     * @param query Search query
     * @param topK Number of top results to return
     * @return List of search results ordered by relevance score (descending)
     * @throws IndexingException if search fails
     */
    List<SearchResult> search(String query, int topK);

    /**
     * Check if index has been built.
     *
     * @return true if index is ready for searching
     */
    boolean isIndexBuilt();

    /**
     * Close the indexer and release resources.
     */
    @Override
    void close();
}
