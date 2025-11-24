package com.mcp.server.retrieval;

import com.mcp.server.core.config.RetrievalConfig;
import com.mcp.server.core.models.SearchResult;
import com.mcp.server.ingest.api.DocumentChunker;
import com.mcp.server.ingest.indexer.LuceneBM25Indexer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaselineRetriever")
class BaselineRetrieverTest {

    @Mock
    private ChromaVectorSearch vectorSearch;

    @Mock
    private LuceneBM25Indexer bm25Indexer;

    @Mock
    private DocumentChunker documentChunker;

    private BaselineRetriever retriever;

    @BeforeEach
    void setUp() {
        RetrievalConfig config = new RetrievalConfig();
        config.setBm25Weight(0.3f);
        config.setVectorWeight(0.7f);
        config.setCandidatePoolSize(20);

        retriever = new BaselineRetriever(vectorSearch, bm25Indexer, documentChunker, config);
    }

    @Nested
    @DisplayName("initialize()")
    class InitializeTests {

        @Test
        @DisplayName("should load BM25 index from disk if it exists")
        void initialize_IndexExists_LoadsIndex() {
            // Arrange
            when(bm25Indexer.indexExistsOnDisk()).thenReturn(true);

            // Act
            retriever.initialize();

            // Assert
            verify(bm25Indexer).indexExistsOnDisk();
            verify(bm25Indexer).loadIndex();
        }

        @Test
        @DisplayName("should handle missing index gracefully")
        void initialize_NoIndex_HandlesGracefully() {
            // Arrange
            when(bm25Indexer.indexExistsOnDisk()).thenReturn(false);

            // Act & Assert - Should not throw
            assertThatNoException().isThrownBy(() -> retriever.initialize());
            verify(bm25Indexer).indexExistsOnDisk();
            verify(bm25Indexer, never()).loadIndex();
        }

        @Test
        @DisplayName("should throw exception when index load fails")
        void initialize_LoadFails_ThrowsException() {
            // Arrange
            when(bm25Indexer.indexExistsOnDisk()).thenReturn(true);
            doThrow(new RuntimeException("Failed to load index"))
                .when(bm25Indexer).loadIndex();

            // Act & Assert
            assertThatThrownBy(() -> retriever.initialize())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Initialization failed");
        }
    }

    @Nested
    @DisplayName("query() with hybrid search")
    class HybridQueryTests {

        @Test
        @DisplayName("should combine BM25 and vector results with weighted scores")
        void query_HybridSearch_CombinesScoresWithWeights() {
            // Arrange
            String query = "AWS Aurora";
            int topK = 2;

            // BM25 results (max score = 10.0)
            List<SearchResult> bm25Results = Arrays.asList(
                new SearchResult("id1", "Aurora is a database", "aurora.md", 10.0f),
                new SearchResult("id2", "AWS provides cloud services", "aws.md", 5.0f)
            );
            when(bm25Indexer.search(query, 20)).thenReturn(bm25Results);

            // Vector results
            Document doc1 = createDocument("Aurora is a database", "aurora.md");
            Document doc2 = createDocument("RDS is a database service", "rds.md");
            List<VectorSearchResult> vectorResults = Arrays.asList(
                new VectorSearchResult(doc1, 0.1f), // similarity = 1/(1+0.1) = 0.909
                new VectorSearchResult(doc2, 0.3f)  // similarity = 1/(1+0.3) = 0.769
            );
            when(vectorSearch.search(query, 20, null)).thenReturn(vectorResults);

            // Act
            List<Map<String, Object>> results = retriever.query(query, topK, true, null);

            // Assert
            assertThat(results).hasSize(2);

            // First result should have the highest hybrid score
            // BM25 normalized: 10.0/10.0 = 1.0, weighted: 0.3 * 1.0 = 0.3
            // Vector similarity: 0.909, weighted: 0.7 * 0.909 = 0.636
            // Hybrid = 0.3 + 0.636 = 0.936
            Map<String, Object> firstResult = results.get(0);
            assertThat(firstResult.get("content")).isEqualTo("Aurora is a database");
            assertThat((Double) firstResult.get("confidence")).isGreaterThan(0.8);

            verify(bm25Indexer).search(query, 20);
            verify(vectorSearch).search(query, 20, null);
        }

        @Test
        @DisplayName("should handle documents that appear in both BM25 and vector results")
        void query_HybridSearch_CombinesScoresForSameDocument() {
            // Arrange
            String query = "test query";

            // Same document in both results
            List<SearchResult> bm25Results = Collections.singletonList(
                new SearchResult("id1", "Test content", "test.md", 8.0f)
            );
            when(bm25Indexer.search(anyString(), anyInt())).thenReturn(bm25Results);

            Document doc = createDocument("Test content", "test.md");
            List<VectorSearchResult> vectorResults = Collections.singletonList(
                new VectorSearchResult(doc, 0.2f)
            );
            when(vectorSearch.search(anyString(), anyInt(), any())).thenReturn(vectorResults);

            // Act
            List<Map<String, Object>> results = retriever.query(query, 5, true, null);

            // Assert
            assertThat(results).hasSize(1);
            // Document appears in both searches, should have combined score
            Double confidence = (Double) results.get(0).get("confidence");
            assertThat(confidence).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("should respect topK parameter")
        void query_HybridSearchWithTopK_ReturnsCorrectNumber() {
            // Arrange
            String query = "test";
            int topK = 3;

            // More than topK results
            List<SearchResult> bm25Results = Arrays.asList(
                new SearchResult("1", "Content 1", "f1.md", 10f),
                new SearchResult("2", "Content 2", "f2.md", 9f),
                new SearchResult("3", "Content 3", "f3.md", 8f),
                new SearchResult("4", "Content 4", "f4.md", 7f),
                new SearchResult("5", "Content 5", "f5.md", 6f)
            );
            when(bm25Indexer.search(anyString(), anyInt())).thenReturn(bm25Results);

            List<VectorSearchResult> vectorResults = Collections.emptyList();
            when(vectorSearch.search(anyString(), anyInt(), any())).thenReturn(vectorResults);

            // Act
            List<Map<String, Object>> results = retriever.query(query, topK, true, null);

            // Assert
            assertThat(results).hasSize(topK);
        }

        @Test
        @DisplayName("should return empty list when no results from either source")
        void query_HybridSearchNoResults_ReturnsEmptyList() {
            // Arrange
            when(bm25Indexer.search(anyString(), anyInt())).thenReturn(Collections.emptyList());
            when(vectorSearch.search(anyString(), anyInt(), any())).thenReturn(Collections.emptyList());

            // Act
            List<Map<String, Object>> results = retriever.query("no match query", 5, true, null);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should normalize BM25 scores by max score")
        void query_HybridSearch_NormalizesBM25Scores() {
            // Arrange
            String query = "test";

            // BM25 with different scores (max = 20.0)
            List<SearchResult> bm25Results = Arrays.asList(
                new SearchResult("1", "High score", "f1.md", 20.0f),
                new SearchResult("2", "Low score", "f2.md", 5.0f)
            );
            when(bm25Indexer.search(anyString(), anyInt())).thenReturn(bm25Results);
            when(vectorSearch.search(anyString(), anyInt(), any())).thenReturn(Collections.emptyList());

            // Act
            List<Map<String, Object>> results = retriever.query(query, 5, true, null);

            // Assert
            assertThat(results).hasSize(2);
            // First result: normalized = 20/20 = 1.0, weighted = 0.3 * 1.0 = 0.3
            Double confidence1 = (Double) results.get(0).get("confidence");
            // Second result: normalized = 5/20 = 0.25, weighted = 0.3 * 0.25 = 0.075
            Double confidence2 = (Double) results.get(1).get("confidence");
            assertThat(confidence1).isGreaterThan(confidence2);
        }
    }

    @Nested
    @DisplayName("query() with vector-only search")
    class VectorOnlyQueryTests {

        @Test
        @DisplayName("should return vector results without BM25")
        void query_VectorOnly_ReturnsVectorResults() {
            // Arrange
            String query = "semantic search";
            int topK = 3;

            Document doc1 = createDocument("Content 1", "file1.md");
            Document doc2 = createDocument("Content 2", "file2.md");
            List<VectorSearchResult> vectorResults = Arrays.asList(
                new VectorSearchResult(doc1, 0.1f),
                new VectorSearchResult(doc2, 0.2f)
            );
            when(vectorSearch.search(query, topK, null)).thenReturn(vectorResults);

            // Act
            List<Map<String, Object>> results = retriever.query(query, topK, false, null);

            // Assert
            assertThat(results).hasSize(2);
            assertThat(results.get(0).get("content")).isEqualTo("Content 1");
            assertThat(results.get(1).get("content")).isEqualTo("Content 2");

            verify(vectorSearch).search(query, topK, null);
            verify(bm25Indexer, never()).search(anyString(), anyInt());
        }

        @Test
        @DisplayName("should convert distance to confidence score")
        void query_VectorOnly_ConvertsDistanceToConfidence() {
            // Arrange
            Document doc = createDocument("Test content", "test.md");
            List<VectorSearchResult> vectorResults = Collections.singletonList(
                new VectorSearchResult(doc, 0.5f) // distance = 0.5
            );
            when(vectorSearch.search(anyString(), anyInt(), any())).thenReturn(vectorResults);

            // Act
            List<Map<String, Object>> results = retriever.query("test", 1, false, null);

            // Assert
            assertThat(results).hasSize(1);
            // Confidence = 1 / (1 + 0.5) = 0.666...
            Double confidence = (Double) results.get(0).get("confidence");
            assertThat(confidence).isCloseTo(0.666, within(0.01));
        }
    }

    @Nested
    @DisplayName("addDocuments()")
    class AddDocumentsTests {

        @Test
        @DisplayName("should add documents to vector store and rebuild BM25 index")
        void addDocuments_ValidDocuments_AddsAndRebuildsIndex() {
            // Arrange
            List<Document> newDocuments = Arrays.asList(
                createDocument("New doc 1", "new1.md"),
                createDocument("New doc 2", "new2.md")
            );

            List<Document> allDocuments = Arrays.asList(
                createDocument("Existing doc", "old.md"),
                createDocument("New doc 1", "new1.md"),
                createDocument("New doc 2", "new2.md")
            );
            when(vectorSearch.getAllDocuments()).thenReturn(allDocuments);

            // Act
            retriever.addDocuments(newDocuments);

            // Assert
            verify(vectorSearch).addDocuments(newDocuments);
            verify(vectorSearch).getAllDocuments();
            verify(bm25Indexer).buildIndex(allDocuments);
        }

        @Test
        @DisplayName("should throw exception when add fails")
        void addDocuments_AddFails_ThrowsException() {
            // Arrange
            List<Document> documents = Collections.singletonList(createDocument("Test", "test.md"));
            doThrow(new RuntimeException("Add failed")).when(vectorSearch).addDocuments(any());

            // Act & Assert
            assertThatThrownBy(() -> retriever.addDocuments(documents))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to add documents");
        }
    }

    @Nested
    @DisplayName("chunkDocuments()")
    class ChunkDocumentsTests {

        @Test
        @DisplayName("should delegate to document chunker")
        void chunkDocuments_ValidDocuments_DelegatesToChunker() {
            // Arrange
            List<Document> documents = List.of(
                    createDocument("Long content to be chunked", "doc.md")
            );
            List<Document> chunks = Arrays.asList(
                createDocument("Chunk 1", "doc.md"),
                createDocument("Chunk 2", "doc.md")
            );
            when(documentChunker.chunkDocuments(documents)).thenReturn(chunks);

            // Act
            List<Document> result = retriever.chunkDocuments(documents);

            // Assert
            assertThat(result).hasSize(2);
            verify(documentChunker).chunkDocuments(documents);
        }
    }

    @Nested
    @DisplayName("Configuration validation")
    class ConfigurationTests {

        @Test
        @DisplayName("should throw exception when weights don't sum to 1.0")
        void constructor_InvalidWeights_ThrowsException() {
            // Arrange
            RetrievalConfig invalidConfig = new RetrievalConfig();
            invalidConfig.setBm25Weight(0.5f);
            invalidConfig.setVectorWeight(0.6f); // Sum = 1.1, invalid

            // Act & Assert
            assertThatThrownBy(() -> new BaselineRetriever(vectorSearch, bm25Indexer, documentChunker, invalidConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must sum to 1.0");
        }

        @Test
        @DisplayName("should accept valid weight configuration")
        void constructor_ValidWeights_CreatesRetriever() {
            // Arrange
            RetrievalConfig validConfig = new RetrievalConfig();
            validConfig.setBm25Weight(0.4f);
            validConfig.setVectorWeight(0.6f); // Sum = 1.0, valid

            // Act & Assert
            assertThatNoException().isThrownBy(() ->
                new BaselineRetriever(vectorSearch, bm25Indexer, documentChunker, validConfig)
            );
        }
    }

    // Helper methods
    private Document createDocument(String content, String filename) {
        Metadata metadata = new Metadata();
        metadata.put("filename", filename);
        metadata.put("source", "/path/to/" + filename);
        metadata.put("type", "personal_note");
        return Document.from(content, metadata);
    }
}
