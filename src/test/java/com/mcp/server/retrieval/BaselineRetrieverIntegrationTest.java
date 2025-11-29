package com.mcp.server.retrieval;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.*;
import org.testcontainers.chromadb.ChromaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for BaselineRetriever with real ChromaDB.
 * Uses TestContainers to spin up a real ChromaDB instance.
 * Tests the complete retrieval pipeline end-to-end.
 */
@Testcontainers
@DisplayName("BaselineRetriever Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BaselineRetrieverIntegrationTest {

    @Container
    static ChromaDBContainer chromaContainer = new ChromaDBContainer("chromadb/chroma:0.4.23");

    private static BaselineRetriever retriever;
    private static final String COLLECTION_NAME = "test_integration_collection";

    @BeforeAll
    static void setUpAll() {
        // Create retriever with TestContainer connection (using test method for in-memory BM25)
        retriever = RetrieverFactory.createTestRetriever(
                chromaContainer.getHost(),
                chromaContainer.getFirstMappedPort(),
                COLLECTION_NAME
        );

        // Ingest test documents once for all tests
        List<Document> documents = Arrays.asList(
                createDocument(
                        "AWS Aurora is a MySQL and PostgreSQL-compatible relational database. " +
                                "Aurora provides up to 5 times better performance than MySQL with the security, " +
                                "availability, and reliability of a commercial database at 1/10th the cost.",
                        "aurora.md",
                        "technical_doc"
                ),
                createDocument(
                        "The CAP theorem states that a distributed system can only guarantee two of three properties: " +
                                "Consistency, Availability, and Partition tolerance. This is a fundamental trade-off in " +
                                "distributed systems design.",
                        "cap_theorem.md",
                        "personal_note"
                ),
                createDocument(
                        "Kubernetes is a container orchestration platform. It automates deployment, scaling, and " +
                                "management of containerized applications. Kubernetes was originally designed by Google.",
                        "kubernetes.md",
                        "technical_doc"
                )
        );

        retriever.addDocuments(documents);
    }

    @Nested
    @Order(1)
    @DisplayName("End-to-end ingestion and retrieval")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EndToEndTests {

        @Test
        @Order(1)
        @DisplayName("should search ingested documents")
        void ingestAndSearch_MultipleDocuments_MakesSearchable() {
            // Documents already ingested in @BeforeAll
            // Assert - Query for Aurora
            List<Map<String, Object>> auroraResults = retriever.query("AWS Aurora database", 3, true);
            assertThat(auroraResults).isNotEmpty();
            assertThat(auroraResults.getFirst().get("content").toString()).contains("Aurora");

            // Assert - Query for CAP theorem
            List<Map<String, Object>> capResults = retriever.query("CAP theorem distributed systems", 3, true);
            assertThat(capResults).isNotEmpty();
            assertThat(capResults.getFirst().get("content").toString()).contains("CAP theorem");

            // Assert - Query for Kubernetes
            List<Map<String, Object>> k8sResults = retriever.query("container orchestration Kubernetes", 3, true);
            assertThat(k8sResults).isNotEmpty();
            assertThat(k8sResults.getFirst().get("content").toString()).contains("Kubernetes");
        }

        @Test
        @Order(2)
        @DisplayName("should perform hybrid search combining BM25 and vector similarity")
        void hybridSearch_KeywordAndSemantic_CombinesResults() {
            // Arrange - Documents already ingested in previous test

            // Act - Hybrid search
            List<Map<String, Object>> hybridResults = retriever.query("database performance", 5, true);

            // Assert - Should find Aurora (has both keywords)
            assertThat(hybridResults).isNotEmpty();
            boolean foundAurora = hybridResults.stream()
                    .anyMatch(r -> r.get("content").toString().contains("Aurora"));
            assertThat(foundAurora).isTrue();

            // All results should have confidence scores
            for (Map<String, Object> result : hybridResults) {
                assertThat(result).containsKey("confidence");
                assertThat((Double) result.get("confidence")).isGreaterThan(0.0);
            }
        }

        @Test
        @Order(3)
        @DisplayName("should perform vector-only search for semantic similarity")
        void vectorOnlySearch_SemanticQuery_FindsRelevantDocs() {
            // Arrange - Documents already ingested

            // Act - Vector-only search (no BM25)
            List<Map<String, Object>> vectorResults = retriever.query("managing containers at scale", 3, false);

            // Assert
            assertThat(vectorResults).isNotEmpty();
            // Should find Kubernetes doc based on semantic similarity
            boolean foundK8s = vectorResults.stream()
                    .anyMatch(r -> r.get("content").toString().contains("Kubernetes"));
            assertThat(foundK8s).isTrue();
        }

        @Test
        @Order(4)
        @DisplayName("should return results sorted by relevance score")
        void query_MultipleResults_SortedByConfidence() {
            // Arrange - Documents already ingested

            // Act
            List<Map<String, Object>> results = retriever.query("AWS database", 3, true);

            // Assert - Results should be sorted by confidence (descending)
            assertThat(results).isNotEmpty();
            for (int i = 0; i < results.size() - 1; i++) {
                Double confidence1 = (Double) results.get(i).get("confidence");
                Double confidence2 = (Double) results.get(i + 1).get("confidence");
                assertThat(confidence1).isGreaterThanOrEqualTo(confidence2);
            }
        }

        @Test
        @Order(5)
        @DisplayName("should respect topK parameter")
        void query_WithTopK_ReturnsCorrectNumberOfResults() {
            // Arrange - 3 documents ingested

            // Act - Request only 2 results
            List<Map<String, Object>> results = retriever.query("system", 2, true);

            // Assert
            assertThat(results).hasSizeLessThanOrEqualTo(2);
        }

        @Test
        @Order(6)
        @DisplayName("should handle queries with no matches")
        void query_NoMatches_ReturnsEmptyList() {
            // Arrange - Documents about Aurora, CAP, Kubernetes

            // Act - Query for something completely different
            List<Map<String, Object>> results = retriever.query(
                    "quantum entanglement particle physics",
                    5,
                    true);

            // Assert - May return some results with very low confidence, or empty
            // Either way, confidence should be low if results exist
            if (!results.isEmpty()) {
                Double topConfidence = (Double) results.getFirst().get("confidence");
                assertThat(topConfidence).isLessThan(0.5); // Low confidence for unrelated query
            }
        }

        @Test
        @Order(7)
        @DisplayName("should preserve metadata in search results")
        void query_WithMetadata_PreservesMetadata() {
            // Arrange - Documents with metadata already ingested

            // Act
            List<Map<String, Object>> results = retriever.query("Aurora", 1, true);

            // Assert
            assertThat(results).isNotEmpty();
            Map<String, Object> firstResult = results.getFirst();
            assertThat(firstResult).containsKey("metadata");

            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) firstResult.get("metadata");
            assertThat(metadata).containsKey("filename");
            // Note: Some metadata fields may be filtered by ChromaDB
        }
    }

    @Nested
    @Order(2)
    @DisplayName("Performance tests")
    class PerformanceTests {

        @Test
        @DisplayName("should complete hybrid search within reasonable time")
        @Timeout(5)
            // 5 seconds max
        void query_HybridSearch_CompletesQuickly() {
            // Act
            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> results = retriever.query("database", 5, true);
            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(results).isNotNull();
            assertThat(duration).isLessThan(1000); // Should complete in < 1 second
        }

        @Test
        @DisplayName("should handle multiple concurrent queries")
        void query_Concurrent_HandlesMultipleQueries() {
            // Arrange
            String[] queries = {
                    "database performance",
                    "distributed systems",
                    "container orchestration"
            };

            // Act - Execute multiple queries in parallel
            List<List<Map<String, Object>>> results = Arrays.stream(queries)
                    .parallel()
                    .map(query -> retriever.query(query, 3, true))
                    .toList();

            // Assert - All queries should complete successfully
            assertThat(results).hasSize(3);
            for (List<Map<String, Object>> result : results) {
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    @Order(3)
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle empty query gracefully")
        void query_EmptyQuery_HandlesGracefully() {
            // Act
            List<Map<String, Object>> results = retriever.query("", 5, true);

            // Assert - Should not throw, may return empty or all docs
            assertThat(results).isNotNull();
        }

        @Test
        @DisplayName("should handle initialization before documents added")
        void initialize_NoDocuments_HandlesGracefully() {
            // Arrange - Create a new retriever with empty collection
            BaselineRetriever emptyRetriever = RetrieverFactory.createTestRetriever(
                    chromaContainer.getHost(),
                    chromaContainer.getFirstMappedPort(),
                    "empty_collection"
            );

            // Act & Assert - Should not throw
            assertThatNoException().isThrownBy(emptyRetriever::initialize);
        }
    }

    // Helper method
    private static Document createDocument(String content, String filename, String type) {
        Metadata metadata = new Metadata();
        metadata.put("filename", filename);
        metadata.put("source", "/test/docs/" + filename);
        metadata.put("doc_type", type);  // Use doc_type instead of type (ChromaDB reserved field)
        return Document.from(content, metadata);
    }
}
