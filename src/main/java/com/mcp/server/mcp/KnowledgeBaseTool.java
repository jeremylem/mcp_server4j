package com.mcp.server.mcp;

import com.mcp.server.core.interfaces.QueryService;
import com.mcp.server.mcp.models.KnowledgeBaseDocument;
import com.mcp.server.mcp.models.KnowledgeBaseOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP tool for querying the knowledge base.
 * Exposes hybrid search (BM25 + vector) through the Model Context Protocol.
 */
@Component
public class KnowledgeBaseTool {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseTool.class);

    private final QueryService queryService;

    public KnowledgeBaseTool(QueryService queryService) {
        this.queryService = queryService;
        logger.info("KnowledgeBaseTool initialized with QueryService");
    }

    /**
     * Query the knowledge base using hybrid search (BM25 + Vector).
     *
     * @param query The search query or question
     * @param topK Number of results to return (default: 5)
     * @param useHybrid Use hybrid search (true) or vector-only (false) (default: true)
     * @return Documents with confidence scores
     */
    @McpTool(name = "query_knowledge_base", description = "Query the knowledge base using hybrid search (BM25 + Vector) to find relevant documents")
    public KnowledgeBaseOutput queryKnowledgeBase(
            @McpToolParam(description = "The search query or question", required = true) String query,
            @McpToolParam(description = "Number of results to return (default: 5)") Integer topK,
            @McpToolParam(description = "Use hybrid search (true) or vector-only (false, default: true)") Boolean useHybrid) {
        int k = topK != null ? topK : 5;
        boolean hybrid = useHybrid != null ? useHybrid : true;

        logger.info("MCP tool invoked: query='{}', topK={}, hybrid={}", query, k, hybrid);

        try {
            // Query using QueryService interface
            List<Map<String, Object>> results = queryService.query(query, k, hybrid);

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
}
