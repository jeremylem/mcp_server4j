package com.mcp.server.core.interfaces;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Manages documents in the knowledge base.
 */
public interface DocumentManager {

    void addDocuments(List<Document> documents);
}
