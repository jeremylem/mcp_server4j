package com.mcp.server.core.interfaces;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Service for chunking documents into smaller segments.
 */
public interface DocumentChunker {

    List<Document> chunkDocuments(List<Document> documents);
}
