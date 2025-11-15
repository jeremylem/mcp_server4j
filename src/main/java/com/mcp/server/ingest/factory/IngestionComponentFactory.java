package com.mcp.server.ingest.factory;

import com.mcp.server.core.config.IngestConfig;
import com.mcp.server.ingest.api.*;

/**
 * Factory for creating ingestion pipeline components.
 *
 * This interface allows swapping implementations (e.g., for testing or different deployments).
 * Follows the Factory pattern for component creation.
 */
public interface IngestionComponentFactory {

    /**
     * Create a document finder.
     *
     * @return DocumentFinder implementation
     */
    DocumentFinder createDocumentFinder();

    /**
     * Create a document loader.
     *
     * @return DocumentLoader implementation
     */
    DocumentLoader createDocumentLoader();

    /**
     * Create a document chunker with the given configuration.
     *
     * @param config Ingestion configuration
     * @return DocumentChunker implementation
     */
    DocumentChunker createDocumentChunker(IngestConfig config);

    /**
     * Create a keyword indexer.
     *
     * @return KeywordIndexer implementation
     */
    KeywordIndexer createKeywordIndexer();

    /**
     * Create a vector store.
     *
     * @param chromaHost ChromaDB host
     * @param chromaPort ChromaDB port
     * @param collectionName Collection name
     * @param config Ingestion configuration
     * @return VectorStore implementation
     */
    VectorStore createVectorStore(String chromaHost, int chromaPort, String collectionName, IngestConfig config);
}
