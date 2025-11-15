package com.mcp.server.ingest.api;

import com.mcp.server.ingest.exception.VectorStoreException;
import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Stores document embeddings for semantic search.
 *
 * Implementations should handle embedding generation and storage
 * in a vector database (e.g., ChromaDB, Pinecone, Weaviate).
 */
public interface VectorStore {

    /**
     * Add documents to the vector store.
     *
     * Implementations should handle:
     * - Document chunking (if not already chunked)
     * - Embedding generation
     * - Storage in the vector database
     *
     * @param documents Documents to add
     * @throws VectorStoreException if storage fails
     */
    void addDocuments(List<Document> documents);

    /**
     * Get count of documents in the store.
     *
     * @return Number of documents/chunks stored
     * @throws VectorStoreException if count operation fails
     */
    int count();

    /**
     * Delete the collection and recreate it.
     *
     * Used for re-ingestion scenarios where you want to start fresh.
     *
     * @throws VectorStoreException if reset fails
     */
    void reset();
}
