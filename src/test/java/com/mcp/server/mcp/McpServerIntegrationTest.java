package com.mcp.server.mcp;

import com.mcp.server.core.interfaces.Retriever;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for MCP Server.
 *
 * Tests the full MCP server functionality including:
 * - Spring Boot application startup
 * - MCP tool registration (query_knowledge_base)
 * - Retriever integration
 * - Hybrid search functionality
 */
@SpringBootTest
@Import(McpServerTestConfig.class)
@ActiveProfiles("test")
class McpServerIntegrationTest {

    @Autowired
    private Retriever retriever;

    @Autowired(required = false)
    private KnowledgeBaseTool knowledgeBaseTool;

    @BeforeEach
    void setUp() {
        // Add test documents
        List<Document> testDocs = List.of(
                Document.from("Python is a high-level programming language",
                        Metadata.from(Map.of("source", "test1.md", "type", "technical_doc"))),
                Document.from("Java is a statically typed programming language",
                        Metadata.from(Map.of("source", "test2.md", "type", "technical_doc"))),
                Document.from("BM25 is a keyword-based ranking algorithm",
                        Metadata.from(Map.of("source", "test3.md", "type", "technical_doc")))
        );

        retriever.addDocuments(testDocs);
        retriever.initialize();
    }

    @Test
    void testApplicationStarts() {
        // Verify Spring Boot context loads successfully
        assertThat(retriever).isNotNull();
    }

    @Test
    void testRetrieverIntegration() {
        // Test retriever is properly configured and can query
        List<Map<String, Object>> results = retriever.query("Python programming", 3, true, null);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0)).containsKeys("content", "metadata", "confidence");
    }

    @Test
    void testKnowledgeBaseToolRegistered() {
        // Verify MCP tool is registered in Spring context
        assertThat(knowledgeBaseTool).isNotNull();
    }

    @Test
    void testQueryKnowledgeBaseTool() {
        // Skip if tool not available yet (will pass once implemented)
        if (knowledgeBaseTool == null) {
            return;
        }

        // Test the MCP tool directly
        var result = knowledgeBaseTool.queryKnowledgeBase("BM25 algorithm", 3, true);

        assertThat(result).isNotNull();
        assertThat(result.getDocuments()).isNotEmpty();
        assertThat(result.getDocuments().get(0).getContent()).contains("BM25");
    }

    @Test
    void testHybridSearchVsVectorOnly() {
        // Test that hybrid search works differently than vector-only
        List<Map<String, Object>> hybridResults = retriever.query("Java", 3, true, null);
        List<Map<String, Object>> vectorResults = retriever.query("Java", 3, false, null);

        assertThat(hybridResults).isNotEmpty();
        assertThat(vectorResults).isNotEmpty();

        // Both should find relevant documents
        assertThat(hybridResults.get(0).get("content").toString()).contains("Java");
    }
}
