package com.mcp.server.retrieval;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChromaVectorSearch")
class ChromaVectorSearchTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    private ChromaVectorSearch vectorSearch;

    @BeforeEach
    void setUp() {
        vectorSearch = new ChromaVectorSearch(embeddingStore, embeddingModel);
    }

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("should return vector search results with distance scores")
        void search_ValidQuery_ReturnsResults() {
            // Arrange
            String query = "AWS Aurora failover";
            int topK = 5;

            // Create mock embedding
            float[] embeddingVector = new float[]{0.1f, 0.2f, 0.3f};
            Embedding queryEmbedding = new Embedding(embeddingVector);
            Response<Embedding> embeddingResponse = Response.from(queryEmbedding);
            when(embeddingModel.embed(query)).thenReturn(embeddingResponse);

            // Create mock search results
            TextSegment segment1 = TextSegment.from("Aurora has 30 second failover",
                createMetadata("aurora.md", "technical_doc"));
            EmbeddingMatch<TextSegment> match1 = new EmbeddingMatch<>(0.95, "id1", queryEmbedding, segment1);

            TextSegment segment2 = TextSegment.from("Aurora is a managed database",
                createMetadata("aws.md", "personal_note"));
            EmbeddingMatch<TextSegment> match2 = new EmbeddingMatch<>(0.85, "id2", queryEmbedding, segment2);

            List<EmbeddingMatch<TextSegment>> matches = Arrays.asList(match1, match2);
            EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(matches);

            when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

            // Act
            List<VectorSearchResult> results = vectorSearch.search(query, topK, null);

            // Assert
            assertThat(results).hasSize(2);

            VectorSearchResult result1 = results.get(0);
            assertThat(result1.getDocument().text()).isEqualTo("Aurora has 30 second failover");
            assertThat(result1.getDistance()).isEqualTo(0.05f); // 1 - 0.95

            VectorSearchResult result2 = results.get(1);
            assertThat(result2.getDocument().text()).isEqualTo("Aurora is a managed database");
            assertThat(result2.getDistance()).isEqualTo(0.15f); // 1 - 0.85

            verify(embeddingModel).embed(query);
            verify(embeddingStore).search(any(EmbeddingSearchRequest.class));
        }

        @Test
        @DisplayName("should create search request with correct topK")
        void search_WithTopK_CreatesRequestWithCorrectLimit() {
            // Arrange
            String query = "test query";
            int topK = 10;

            float[] embeddingVector = new float[]{0.1f, 0.2f};
            Embedding queryEmbedding = new Embedding(embeddingVector);
            when(embeddingModel.embed(query)).thenReturn(Response.from(queryEmbedding));
            when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

            ArgumentCaptor<EmbeddingSearchRequest> requestCaptor =
                ArgumentCaptor.forClass(EmbeddingSearchRequest.class);

            // Act
            vectorSearch.search(query, topK, null);

            // Assert
            verify(embeddingStore).search(requestCaptor.capture());
            EmbeddingSearchRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.maxResults()).isEqualTo(topK);
        }

        @Test
        @DisplayName("should return empty list when no matches found")
        void search_NoMatches_ReturnsEmptyList() {
            // Arrange
            String query = "nonexistent query";
            float[] embeddingVector = new float[]{0.1f, 0.2f};
            Embedding queryEmbedding = new Embedding(embeddingVector);
            when(embeddingModel.embed(query)).thenReturn(Response.from(queryEmbedding));
            when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

            // Act
            List<VectorSearchResult> results = vectorSearch.search(query, 5, null);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should preserve metadata in results")
        void search_WithMetadata_PreservesMetadata() {
            // Arrange
            String query = "test";
            Metadata metadata = createMetadata("test.md", "personal_note");
            metadata.put("custom_field", "custom_value");

            float[] embeddingVector = new float[]{0.1f};
            Embedding queryEmbedding = new Embedding(embeddingVector);
            when(embeddingModel.embed(query)).thenReturn(Response.from(queryEmbedding));

            TextSegment segment = TextSegment.from("content", metadata);
            EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "id1", queryEmbedding, segment);
            when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(Collections.singletonList(match)));

            // Act
            List<VectorSearchResult> results = vectorSearch.search(query, 1, null);

            // Assert
            assertThat(results).hasSize(1);
            Map<String, String> resultMetadata = ChromaVectorSearch.metadataToMap(
                results.get(0).getDocument().metadata()
            );
            assertThat(resultMetadata).containsEntry("filename", "test.md");
            assertThat(resultMetadata).containsEntry("type", "personal_note");
            assertThat(resultMetadata).containsEntry("custom_field", "custom_value");
        }
    }

    @Nested
    @DisplayName("addDocuments()")
    class AddDocumentsTests {

        @Test
        @DisplayName("should add documents with embeddings to store")
        void addDocuments_ValidDocuments_AddsToStore() {
            // Arrange
            Document doc1 = Document.from("First document", createMetadata("doc1.md", "note"));
            Document doc2 = Document.from("Second document", createMetadata("doc2.md", "note"));
            List<Document> documents = Arrays.asList(doc1, doc2);

            float[] embedding1 = new float[]{0.1f, 0.2f};
            float[] embedding2 = new float[]{0.3f, 0.4f};
            when(embeddingModel.embed("First document")).thenReturn(Response.from(new Embedding(embedding1)));
            when(embeddingModel.embed("Second document")).thenReturn(Response.from(new Embedding(embedding2)));

            // Act
            vectorSearch.addDocuments(documents);

            // Assert
            verify(embeddingModel).embed("First document");
            verify(embeddingModel).embed("Second document");
            verify(embeddingStore).addAll(anyList(), anyList());
        }

        @Test
        @DisplayName("should generate embeddings for each document")
        void addDocuments_MultipleDocuments_GeneratesEmbeddingsForEach() {
            // Arrange
            List<Document> documents = Arrays.asList(
                Document.from("Doc 1", new Metadata()),
                Document.from("Doc 2", new Metadata()),
                Document.from("Doc 3", new Metadata())
            );

            float[] dummyEmbedding = new float[]{0.1f};
            when(embeddingModel.embed(anyString())).thenReturn(Response.from(new Embedding(dummyEmbedding)));

            // Act
            vectorSearch.addDocuments(documents);

            // Assert
            verify(embeddingModel, times(3)).embed(anyString());
            verify(embeddingModel).embed("Doc 1");
            verify(embeddingModel).embed("Doc 2");
            verify(embeddingModel).embed("Doc 3");
        }

        @Test
        @DisplayName("should throw exception when embedding fails")
        void addDocuments_EmbeddingFails_ThrowsException() {
            // Arrange
            Document doc = Document.from("Test", new Metadata());
            when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("Embedding failed"));

            // Act & Assert
            assertThatThrownBy(() -> vectorSearch.addDocuments(Collections.singletonList(doc)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to add documents to vector store");
        }
    }

    @Nested
    @DisplayName("getAllDocuments()")
    class GetAllDocumentsTests {

        @Test
        @DisplayName("should fetch all documents from store")
        void getAllDocuments_DocumentsExist_ReturnsAllDocuments() {
            // Arrange
            float[] dummyEmbedding = new float[]{0.1f};
            Embedding queryEmbedding = new Embedding(dummyEmbedding);
            when(embeddingModel.embed("document")).thenReturn(Response.from(queryEmbedding));

            TextSegment segment1 = TextSegment.from("Content 1", createMetadata("file1.md", "note"));
            TextSegment segment2 = TextSegment.from("Content 2", createMetadata("file2.md", "doc"));

            List<EmbeddingMatch<TextSegment>> matches = Arrays.asList(
                new EmbeddingMatch<>(0.9, "id1", queryEmbedding, segment1),
                new EmbeddingMatch<>(0.8, "id2", queryEmbedding, segment2)
            );
            when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(matches));

            // Act
            List<Document> documents = vectorSearch.getAllDocuments();

            // Assert
            assertThat(documents).hasSize(2);
            assertThat(documents.get(0).text()).isEqualTo("Content 1");
            assertThat(documents.get(1).text()).isEqualTo("Content 2");
        }

        @Test
        @DisplayName("should return empty list when no documents exist")
        void getAllDocuments_NoDocuments_ReturnsEmptyList() {
            // Arrange
            float[] dummyEmbedding = new float[]{0.1f};
            when(embeddingModel.embed("document")).thenReturn(Response.from(new Embedding(dummyEmbedding)));
            when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(Collections.emptyList()));

            // Act
            List<Document> documents = vectorSearch.getAllDocuments();

            // Assert
            assertThat(documents).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when fetch fails")
        void getAllDocuments_FetchFails_ThrowsException() {
            // Arrange
            float[] dummyEmbedding = new float[]{0.1f};
            when(embeddingModel.embed("document")).thenReturn(Response.from(new Embedding(dummyEmbedding)));
            when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenThrow(new RuntimeException("ChromaDB connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> vectorSearch.getAllDocuments())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch documents from ChromaDB");
        }
    }

    @Nested
    @DisplayName("metadataToMap()")
    class MetadataToMapTests {

        @Test
        @DisplayName("should convert metadata to map")
        void metadataToMap_ValidMetadata_ReturnsMap() {
            // Arrange
            Metadata metadata = new Metadata();
            metadata.put("key1", "value1");
            metadata.put("key2", "value2");

            // Act
            Map<String, String> map = ChromaVectorSearch.metadataToMap(metadata);

            // Assert
            assertThat(map).containsEntry("key1", "value1");
            assertThat(map).containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("should return empty map for null metadata")
        void metadataToMap_NullMetadata_ReturnsEmptyMap() {
            // Arrange & Act
            Map<String, String> map = ChromaVectorSearch.metadataToMap(null);

            // Assert
            assertThat(map).isEmpty();
        }

        // Note: Test for null values in metadata removed because LangChain4j's Metadata
        // does not allow null values. This is a design constraint of the library.
    }

    // Helper methods
    private Metadata createMetadata(String filename, String type) {
        Metadata metadata = new Metadata();
        metadata.put("filename", filename);
        metadata.put("type", type);
        return metadata;
    }
}
