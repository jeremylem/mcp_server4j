package com.mcp.server.mcp.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Output from knowledge base query tool.
 *
 * Contains list of relevant documents with metadata and confidence scores.
 * This is the response format returned to MCP clients (like Claude Desktop).
 */
@Setter
@Getter
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

}
