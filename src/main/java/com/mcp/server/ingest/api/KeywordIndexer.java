package com.mcp.server.ingest.api;

import com.mcp.server.core.models.SearchResult;
import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Builds and searches a keyword-based index (e.g., BM25, TF-IDF).
 * <p>
 * Implementations should handle index creation, query parsing, and result ranking.
 */
public interface KeywordIndexer extends AutoCloseable {

    void buildIndex(List<Document> documents);

    List<SearchResult> search(String query, int topK);

    /**
     * Close the indexer and release resources.
     */
    @Override
    void close();
}
