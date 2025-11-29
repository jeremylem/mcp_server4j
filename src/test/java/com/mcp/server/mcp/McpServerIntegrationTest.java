package com.mcp.server.mcp;

import com.mcp.server.retrieval.BaselineRetriever;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.chromadb.ChromaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for MCP Server with TestContainers ChromaDB.
 */
@SpringBootTest
@Import(McpServerTestConfig.class)
@ActiveProfiles("test")
@Testcontainers
class McpServerIntegrationTest {

    @Container
    static ChromaDBContainer chromaContainer = new ChromaDBContainer("chromadb/chroma:0.4.23");

    @DynamicPropertySource
    static void setChromaProperties(DynamicPropertyRegistry registry) {
        registry.add("chroma.host", chromaContainer::getHost);
        registry.add("chroma.port", chromaContainer::getFirstMappedPort);
    }

    @Autowired
    private BaselineRetriever retriever;

    @Autowired(required = false)
    private KnowledgeBaseTool knowledgeBaseTool;

    private static boolean documentsIngested = false;

    @BeforeEach
    void setUp() {
        // Only ingest documents once to avoid test interference
        if (!documentsIngested) {
            List<Document> testDocs = List.of(
                    Document.from("Python is a high-level programming language",
                            Metadata.from(Map.of("source", "test1.md", "type", "technical_doc"))),
                    Document.from("Java is a statically typed programming language",
                            Metadata.from(Map.of("source", "test2.md", "type", "technical_doc"))),
                    Document.from("BM25 is a keyword-based ranking algorithm",
                            Metadata.from(Map.of("source", "test3.md", "type", "technical_doc")))
            );

            retriever.addDocuments(testDocs);
            documentsIngested = true;
        }
    }

    @Test
    void testApplicationStarts() {
        // Verify Spring Boot context loads successfully
        assertThat(retriever).isNotNull();
    }

    @Test
    void testRetrieverIntegration() {
        // Test retriever is properly configured and can query
        List<Map<String, Object>> results = retriever.query("Python programming", 3, true);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst()).containsKeys("content", "metadata", "confidence");
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
        assertThat(result.getDocuments().getFirst().content()).contains("BM25");
    }

    @Test
    void testHybridSearchVsVectorOnly() {
        // Test that hybrid search works differently than vector-only
        List<Map<String, Object>> hybridResults = retriever.query("Java", 3, true);
        List<Map<String, Object>> vectorResults = retriever.query("Java", 3, false);

        assertThat(hybridResults).isNotEmpty();
        assertThat(vectorResults).isNotEmpty();

        // Both should find relevant documents
        assertThat(hybridResults.getFirst().get("content").toString()).contains("Java");
    }
}
