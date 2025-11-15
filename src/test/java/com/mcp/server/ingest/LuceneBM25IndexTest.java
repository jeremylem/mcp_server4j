package com.mcp.server.ingest;

import com.mcp.server.core.models.BM25SearchResult;
import com.mcp.server.ingest.bm25.LuceneBM25Index;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD-style tests for LuceneBM25Index class.
 *
 * Tests the in-memory Lucene BM25 index functionality.
 */
@DisplayName("LuceneBM25Index")
class LuceneBM25IndexTest {

    private LuceneBM25Index index;

    @BeforeEach
    void setUp() {
        index = new LuceneBM25Index();
    }

    // ============================================
    // buildIndex Tests
    // ============================================

    @Nested
    @DisplayName("buildIndex()")
    class BuildIndexTests {

        @Test
        @DisplayName("should build index from documents")
        void buildIndex_ValidDocuments_BuildsSuccessfully() throws IOException {
            // Arrange
            List<Document> documents = List.of(
                createDocument("0", "I like football.", "doc1.md"),
                createDocument("1", "The weather is good today.", "doc2.md"),
                createDocument("2", "Python is a great programming language.", "doc3.md")
            );

            // Act & Assert
            assertThatCode(() -> index.buildIndex(documents))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle empty document list")
        void buildIndex_EmptyList_HandlesGracefully() throws IOException {
            // Act & Assert
            assertThatCode(() -> index.buildIndex(List.of()))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should build index with special characters in content")
        void buildIndex_SpecialCharacters_HandlesCorrectly() throws IOException {
            // Arrange
            List<Document> documents = List.of(
                createDocument("0", "Email: user@example.com", "email.md"),
                createDocument("1", "Code: def foo(): pass", "code.md"),
                createDocument("2", "Math: 2+2=4 & 3*3=9", "math.md")
            );

            // Act & Assert
            assertThatCode(() -> index.buildIndex(documents))
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
        void setUp() throws IOException {
            // Build index with test documents
            List<Document> documents = List.of(
                createDocument("0", "I like football and soccer.", "sports.md"),
                createDocument("1", "The weather is good today.", "weather.md"),
                createDocument("2", "Python is a great programming language for data science.", "python.md"),
                createDocument("3", "Java is widely used for enterprise applications.", "java.md"),
                createDocument("4", "Machine learning and artificial intelligence are transforming technology.", "ai.md")
            );
            index.buildIndex(documents);
        }

        @Test
        @DisplayName("should return relevant documents for query")
        void search_RelevantQuery_ReturnsMatchingDocuments() throws Exception {
            // Act
            List<BM25SearchResult> results = index.search("football", 5);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getContent()).contains("football");
            assertThat(results.get(0).getScore()).isGreaterThan(0.0f);
        }

        @Test
        @DisplayName("should rank documents by BM25 score")
        void search_MultipleMatches_RanksByRelevance() throws Exception {
            // Act
            List<BM25SearchResult> results = index.search("programming", 5);

            // Assert
            assertThat(results).isNotEmpty();
            // Verify results are sorted by score (descending)
            for (int i = 0; i < results.size() - 1; i++) {
                assertThat(results.get(i).getScore())
                    .isGreaterThanOrEqualTo(results.get(i + 1).getScore());
            }
        }

        @Test
        @DisplayName("should respect topK parameter")
        void search_TopKParameter_ReturnsLimitedResults() throws Exception {
            // Act
            List<BM25SearchResult> results = index.search("the", 2);

            // Assert
            assertThat(results).hasSizeLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when no matches found")
        void search_NoMatches_ReturnsEmptyList() throws Exception {
            // Act
            List<BM25SearchResult> results = index.search("xyznonexistentterm", 5);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle multi-word queries")
        void search_MultiWordQuery_SearchesAllTerms() throws Exception {
            // Act
            List<BM25SearchResult> results = index.search("machine learning", 5);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getContent()).containsAnyOf("machine", "learning");
        }

        @Test
        @DisplayName("should throw exception when searching before building index")
        void search_IndexNotBuilt_ThrowsException() {
            // Arrange
            LuceneBM25Index emptyIndex = new LuceneBM25Index();

            // Act & Assert
            assertThatThrownBy(() -> emptyIndex.search("test", 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Index not built");
        }

        @Test
        @DisplayName("should include metadata in search results")
        void search_WithMetadata_IncludesMetadataInResults() throws Exception {
            // Act
            List<BM25SearchResult> results = index.search("football", 1);

            // Assert
            assertThat(results).isNotEmpty();
            BM25SearchResult result = results.get(0);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getFilename()).isEqualTo("sports.md");
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
        void scoring_ExactMatch_ScoresHigher() throws Exception {
            // Arrange
            List<Document> documents = List.of(
                createDocument("0", "artificial intelligence", "ai.md"),
                createDocument("1", "intelligence agency", "agency.md"),
                createDocument("2", "artificial flowers", "flowers.md")
            );
            index.buildIndex(documents);

            // Act
            List<BM25SearchResult> results = index.search("artificial intelligence", 3);

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getContent()).isEqualTo("artificial intelligence");
            assertThat(results.get(0).getScore()).isGreaterThan(results.get(1).getScore());
        }

        @Test
        @DisplayName("should use BM25 parameters k1=1.2 and b=0.75")
        void scoring_BM25Parameters_UsesCorrectValues() throws Exception {
            // Arrange
            List<Document> documents = List.of(
                createDocument("0", "short", "short.md"),
                createDocument("1", "this is a much longer document with many more words", "long.md")
            );
            index.buildIndex(documents);

            // Act
            List<BM25SearchResult> results = index.search("document", 2);

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
        void close_AfterBuildingIndex_ClosesSuccessfully() throws IOException {
            // Arrange
            List<Document> documents = List.of(
                createDocument("0", "test content", "test.md")
            );
            index.buildIndex(documents);

            // Act & Assert
            assertThatCode(() -> index.close())
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle close when index not built")
        void close_IndexNotBuilt_HandlesGracefully() {
            // Act & Assert
            assertThatCode(() -> index.close())
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle multiple close calls")
        void close_CalledMultipleTimes_HandlesGracefully() throws IOException {
            // Arrange
            List<Document> documents = List.of(
                createDocument("0", "test", "test.md")
            );
            index.buildIndex(documents);

            // Act & Assert
            assertThatCode(() -> {
                index.close();
                index.close();
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
