package com.mcp.server.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mcp.server.core.interfaces.QueryService;
import com.mcp.server.mcp.models.KnowledgeBaseDocument;
import com.mcp.server.mcp.models.KnowledgeBaseOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP Tool for querying the knowledge base.
 *
 * Refactored to follow SOLID principles:
 * - Dependency Inversion: Depends on QueryService interface (not concrete Retriever)
 * - Interface Segregation: Only uses query capability, not document management
 *
 * Exposes the RAG system through Model Context Protocol (MCP) so that
 * AI assistants (like Claude Desktop) can search the knowledge base.
 *
 * This tool uses hybrid search (BM25 + Vector) to find relevant documents
 * and returns them with confidence scores.
 */
@Component
public class KnowledgeBaseTool implements Function<KnowledgeBaseTool.Request, KnowledgeBaseOutput> {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseTool.class);

    private final QueryService queryService;

    public KnowledgeBaseTool(QueryService queryService) {
        this.queryService = queryService;
        logger.info("KnowledgeBaseTool initialized with QueryService");
    }

    /**
     * Request object for knowledge base queries.
     */
    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("The search query or question")
            String query,

            @JsonProperty(defaultValue = "5")
            @JsonPropertyDescription("Number of results to return")
            Integer topK,

            @JsonProperty(defaultValue = "true")
            @JsonPropertyDescription("Use hybrid search (true) or vector-only (false)")
            Boolean useHybrid
    ) {}

    /**
     * Query the knowledge base using hybrid search.
     *
     * Combines BM25 keyword search with vector semantic search for
     * robust retrieval across diverse document types.
     *
     * @param request The query request containing search parameters
     * @return KnowledgeBaseOutput containing relevant documents with confidence scores
     */
    @Override
    public KnowledgeBaseOutput apply(Request request) {
        String query = request.query();
        int topK = request.topK() != null ? request.topK() : 5;
        boolean useHybrid = request.useHybrid() != null ? request.useHybrid() : true;

        logger.info("MCP tool invoked: query='{}', topK={}, hybrid={}", query, topK, useHybrid);

        try {
            // Query using QueryService interface
            List<Map<String, Object>> results = queryService.query(query, topK, useHybrid, null);

            // Convert to KnowledgeBaseDocument objects
            List<KnowledgeBaseDocument> documents = results.stream()
                    .map(result -> {
                        String content = (String) result.get("content");
                        @SuppressWarnings("unchecked")
                        Map<String, String> metadata = (Map<String, String>) result.get("metadata");
                        Double confidence = (Double) result.get("confidence");

                        return new KnowledgeBaseDocument(content, metadata, confidence);
                    })
                    .collect(Collectors.toList());

            logger.info("Returning {} documents from knowledge base", documents.size());

            return new KnowledgeBaseOutput(documents);

        } catch (Exception e) {
            logger.error("Error querying knowledge base", e);
            throw new RuntimeException("Failed to query knowledge base: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method for direct invocation (for testing).
     */
    public KnowledgeBaseOutput queryKnowledgeBase(String query, Integer topK, Boolean useHybrid) {
        return apply(new Request(query, topK, useHybrid));
    }
}
