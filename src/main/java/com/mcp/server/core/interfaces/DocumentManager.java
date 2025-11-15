package com.mcp.server.core.interfaces;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Manages documents in the knowledge base.
 *
 * Follows Interface Segregation Principle - focused only on document management.
 * Handles adding, updating, and removing documents from the store.
 */
public interface DocumentManager {

    /**
     * Add documents to the knowledge base.
     *
     * @param documents List of documents to add
     */
    void addDocuments(List<Document> documents);
}
