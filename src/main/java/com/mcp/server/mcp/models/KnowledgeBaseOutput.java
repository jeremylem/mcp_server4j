package com.mcp.server.mcp.models;

import java.util.List;

/**
 * Output from knowledge base query tool.
 *
 * Contains list of relevant documents with metadata and confidence scores.
 * This is the response format returned to MCP clients (like Claude Desktop).
 */
public class KnowledgeBaseOutput {

    private List<KnowledgeBaseDocument> documents;
    private String summary;

    public KnowledgeBaseOutput() {
    }

    public KnowledgeBaseOutput(List<KnowledgeBaseDocument> documents) {
        this.documents = documents;
    }

    public KnowledgeBaseOutput(List<KnowledgeBaseDocument> documents, String summary) {
        this.documents = documents;
        this.summary = summary;
    }

    public List<KnowledgeBaseDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<KnowledgeBaseDocument> documents) {
        this.documents = documents;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
