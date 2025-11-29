package com.mcp.server.ingest.api;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Stores document embeddings for semantic search.
 */
public interface VectorStore {

    void addDocuments(List<Document> documents);

    int count();

    void reset();
}
