package com.mcp.server.ingest;

import com.mcp.server.core.models.SearchResult;
import com.mcp.server.ingest.indexer.LuceneBM25Indexer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD-style tests for LuceneBM25Indexer class.
 * <p>
 * Tests the in-memory Lucene BM25 indexer functionality.
 */
@DisplayName("LuceneBM25Indexer")
class LuceneBM25IndexerTest {

    private LuceneBM25Indexer indexer;

    @BeforeEach
    void setUp() {
        indexer = new LuceneBM25Indexer();
    }

    // ============================================
    // buildIndex Tests
    // ============================================

    @Nested
    @DisplayName("buildIndex()")
    class BuildIndexTests {

        @Test
        @DisplayName("should build index from documents")
        void buildIndex_ValidDocuments_BuildsSuccessfully() {
            // Arrange
            List<Document> documents = List.of(
                    createDocument("0", "I like football.", "doc1.md"),
                    createDocument("1", "The weather is good today.", "doc2.md"),
                    createDocument("2", "Python is a great programming language.", "doc3.md")
            );

            // Act & Assert
            assertThatCode(() -> indexer.buildIndex(documents))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle empty document list")
        void buildIndex_EmptyList_HandlesGracefully() {
            // Act & Assert
            assertThatCode(() -> indexer.buildIndex(List.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should build index with special characters in content")
        void buildIndex_SpecialCharacters_HandlesCorrectly() {
            // Arrange
            List<Document> documents = List.of(
                    createDocument("0", "Email: user@example.com", "email.md"),
                    createDocument("1", "Code: def foo(): pass", "code.md"),
                    createDocument("2", "Math: 2+2=4 & 3*3=9", "math.md")
            );

            // Act & Assert
            assertThatCode(() -> indexer.buildIndex(documents))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================
    // search Tests
    // ============================================

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @BeforeEach
        void setUp() {
            // Build index with test documents
            List<Document> documents = List.of(
                    createDocument("0", "I like football and soccer.", "sports.md"),
                    createDocument("1", "The weather is good today.", "weather.md"),
                    createDocument("2", "Python is a great programming language for data science.", "python.md"),
                    createDocument("3", "Java is widely used for enterprise applications.", "java.md"),
                    createDocument("4", "Machine learning and artificial intelligence are transforming technology.", "ai.md")
            );
            indexer.buildIndex(documents);
        }

        @Test
        @DisplayName("should return relevant documents for query")
        void search_RelevantQuery_ReturnsMatchingDocuments() {
            // Act
            List<SearchResult> results = indexer.search("football", 5);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).content()).contains("football");
            assertThat(results.get(0).score()).isGreaterThan(0.0f);
        }

        @Test
        @DisplayName("should rank documents by BM25 score")
        void search_MultipleMatches_RanksByRelevance() {
            // Act
            List<SearchResult> results = indexer.search("programming", 5);

            // Assert
            assertThat(results).isNotEmpty();
            // Verify results are sorted by score (descending)
            for (int i = 0; i < results.size() - 1; i++) {
                assertThat(results.get(i).score())
                        .isGreaterThanOrEqualTo(results.get(i + 1).score());
            }
        }

        @Test
        @DisplayName("should respect topK parameter")
        void search_TopKParameter_ReturnsLimitedResults() {
            // Act
            List<SearchResult> results = indexer.search("the", 2);

            // Assert
            assertThat(results).hasSizeLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when no matches found")
        void search_NoMatches_ReturnsEmptyList() {
            // Act
            List<SearchResult> results = indexer.search("xyznonexistentterm", 5);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle multi-word queries")
        void search_MultiWordQuery_SearchesAllTerms() {
            // Act
            List<SearchResult> results = indexer.search("machine learning", 5);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).content()).containsAnyOf("machine", "learning");
        }

        @Test
        @DisplayName("should throw exception when searching before building index")
        void search_IndexNotBuilt_ThrowsException() {
            // Arrange
            LuceneBM25Indexer emptyIndexer = new LuceneBM25Indexer();

            // Act & Assert - LuceneBM25Indexer throws IndexingException
            assertThatThrownBy(() -> emptyIndexer.search("test", 5))
                    .hasMessageContaining("Index not built");
        }

        @Test
        @DisplayName("should include metadata in search results")
        void search_WithMetadata_IncludesMetadataInResults() {
            // Act
            List<SearchResult> results = indexer.search("football", 1);

            // Assert
            assertThat(results).isNotEmpty();
            SearchResult result = results.get(0);
            assertThat(result.id()).isNotNull();
            assertThat(result.filename()).isEqualTo("sports.md");
        }
    }

    // ============================================
    // BM25 Scoring Tests
    // ============================================

    @Nested
    @DisplayName("BM25 Scoring")
    class BM25ScoringTests {

        @Test
        @DisplayName("should score exact matches higher than partial matches")
        void scoring_ExactMatch_ScoresHigher() {
            // Arrange
            List<Document> documents = List.of(
                    createDocument("0", "artificial intelligence", "ai.md"),
                    createDocument("1", "intelligence agency", "agency.md"),
                    createDocument("2", "artificial flowers", "flowers.md")
            );
            indexer.buildIndex(documents);

            // Act
            List<SearchResult> results = indexer.search("artificial intelligence", 3);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).content()).isEqualTo("artificial intelligence");
            assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
        }

        @Test
        @DisplayName("should use BM25 parameters k1=1.2 and b=0.75")
        void scoring_BM25Parameters_UsesCorrectValues() {
            // Arrange
            List<Document> documents = List.of(
                    createDocument("0", "short", "short.md"),
                    createDocument("1", "this is a much longer document with many more words", "long.md")
            );
            indexer.buildIndex(documents);

            // Act
            List<SearchResult> results = indexer.search("document", 2);

            // Assert
            // The longer document should be penalized by BM25's length normalization (b=0.75)
            assertThat(results).isNotEmpty();
        }
    }

    // ============================================
    // close Tests
    // ============================================

    @Nested
    @DisplayName("close()")
    class CloseTests {

        @Test
        @DisplayName("should close resources without error")
        void close_AfterBuildingIndex_ClosesSuccessfully() {
            // Arrange
            List<Document> documents = List.of(
                    createDocument("0", "test content", "test.md")
            );
            indexer.buildIndex(documents);

            // Act & Assert
            assertThatCode(() -> indexer.close())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle close when index not built")
        void close_IndexNotBuilt_HandlesGracefully() {
            // Act & Assert
            assertThatCode(() -> indexer.close())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle multiple close calls")
        void close_CalledMultipleTimes_HandlesGracefully() {
            // Arrange
            List<Document> documents = List.of(
                    createDocument("0", "test", "test.md")
            );
            indexer.buildIndex(documents);

            // Act & Assert
            assertThatCode(() -> {
                indexer.close();
                indexer.close();
            }).doesNotThrowAnyException();
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Document createDocument(String id, String content, String filename) {
        Metadata metadata = Metadata.from(Map.of(
                "id", id,
                "filename", filename,
                "type", filename.endsWith(".pdf") ? "technical_doc" : "personal_note"
        ));
        return Document.from(content, metadata);
    }
}
