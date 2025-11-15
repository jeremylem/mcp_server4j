package com.mcp.server.core.interfaces;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Service for chunking documents into smaller segments.
 *
 * Follows Interface Segregation Principle - focused only on document chunking.
 * Separates chunking logic from retrieval and document management concerns.
 */
public interface DocumentChunker {

    /**
     * Chunk documents using a specific chunking strategy.
     *
     * @param documents Raw documents to chunk
     * @return Chunked documents with preserved metadata
     */
    List<Document> chunkDocuments(List<Document> documents);
}
