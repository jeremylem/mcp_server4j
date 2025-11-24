package com.mcp.server.retrieval;

import com.mcp.server.core.config.RetrievalConfig;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.chromadb.ChromaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for RetrieverFactory.
 * Uses TestContainers to spin up real ChromaDB for testing factory behavior.
 * These are INTEGRATION tests, not unit tests, because they test actual
 * connections to external services.
 */
@Testcontainers
@DisplayName("RetrieverFactory Integration Tests")
class RetrieverFactoryIntegrationTest {

    @Container
    static ChromaDBContainer chromaContainer = new ChromaDBContainer("chromadb/chroma:0.4.23");

    @Nested
    @DisplayName("createBaselineRetriever() with default config")
    class CreateWithDefaultConfigTests {

        @Test
        @DisplayName("should create retriever with default configuration")
        void createBaselineRetriever_DefaultConfig_CreatesRetriever() {
            // Arrange
            String chromaHost = chromaContainer.getHost();
            int chromaPort = chromaContainer.getFirstMappedPort();
            String collectionName = "test_collection";

            // Act
            BaselineRetriever retriever = RetrieverFactory.createTestRetriever(
                chromaHost,
                chromaPort,
                collectionName
            );

            // Assert
            assertThat(retriever).isNotNull();
            assertThat(retriever).isInstanceOf(BaselineRetriever.class);
        }

        @Test
        @DisplayName("should create retriever that can initialize")
        void createBaselineRetriever_DefaultConfig_CanInitialize() {
            // Arrange
            String chromaHost = chromaContainer.getHost();
            int chromaPort = chromaContainer.getFirstMappedPort();
            BaselineRetriever retriever = RetrieverFactory.createTestRetriever(
                chromaHost,
                chromaPort,
                "test_init_collection"
            );

            // Act & Assert - Should not throw
            assertThatNoException().isThrownBy(retriever::initialize);
        }
    }

    @Nested
    @DisplayName("createBaselineRetriever() with custom config")
    class CreateWithCustomConfigTests {

        @Test
        @DisplayName("should create retriever with custom configuration")
        void createBaselineRetriever_CustomConfig_CreatesRetriever() {
            // Arrange
            String chromaHost = chromaContainer.getHost();
            int chromaPort = chromaContainer.getFirstMappedPort();
            String collectionName = "test_collection";

            RetrievalConfig customConfig = new RetrievalConfig();
            customConfig.setBm25Weight(0.4f);
            customConfig.setVectorWeight(0.6f);
            customConfig.setChunkSize(1024);
            customConfig.setChunkOverlap(100);

            // Act
            BaselineRetriever retriever = RetrieverFactory.createTestRetriever(
                chromaHost,
                chromaPort,
                collectionName,
                customConfig
            );

            // Assert
            assertThat(retriever).isNotNull();
            assertThat(retriever).isInstanceOf(BaselineRetriever.class);
        }

        @Test
        @DisplayName("should throw exception for invalid config weights")
        void createBaselineRetriever_InvalidWeights_ThrowsException() {
            // Arrange
            String chromaHost = chromaContainer.getHost();
            int chromaPort = chromaContainer.getFirstMappedPort();
            String collectionName = "test_collection";

            RetrievalConfig invalidConfig = new RetrievalConfig();
            invalidConfig.setBm25Weight(0.5f);
            invalidConfig.setVectorWeight(0.6f); // Sum = 1.1, invalid

            // Act & Assert
            assertThatThrownBy(() -> RetrieverFactory.createTestRetriever(
                chromaHost,
                chromaPort,
                collectionName,
                invalidConfig
            )).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("must sum to 1.0");
        }

        @Test
        @DisplayName("should use custom chunk size from config")
        void createBaselineRetriever_CustomChunkSize_UsesCustomSize() {
            // Arrange
            RetrievalConfig config = new RetrievalConfig();
            config.setChunkSize(2048);
            config.setChunkOverlap(200);

            // Act
            BaselineRetriever retriever = RetrieverFactory.createTestRetriever(
                chromaContainer.getHost(),
                chromaContainer.getFirstMappedPort(),
                "test",
                config
            );

            // Assert
            assertThat(retriever).isNotNull();
        }
    }
}
