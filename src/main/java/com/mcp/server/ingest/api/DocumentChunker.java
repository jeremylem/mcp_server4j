package com.mcp.server.ingest.api;

import com.mcp.server.ingest.exception.DocumentChunkException;
import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Splits documents into smaller chunks for indexing and embedding.
 */
public interface DocumentChunker {

    /**
     * Split documents into chunks.
     *
     * @param documents Documents to chunk
     * @return List of document chunks with preserved metadata
     * @throws DocumentChunkException if chunking fails
     */
    List<Document> chunkDocuments(List<Document> documents);
}
