package com.mcp.server.retrieval;

import com.mcp.server.core.config.RetrievalConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RetrievalConfig validation.
 * Note: Factory creation tests that require ChromaDB connection have been moved
 * to RetrieverFactoryIntegrationTest.java to avoid connection failures in unit tests.
 * These tests focus on configuration validation without external dependencies.
 */
@DisplayName("RetrieverFactory Unit Tests")
class RetrieverFactoryTest {

    @Nested
    @DisplayName("Configuration defaults")
    class ConfigurationDefaultsTests {

        @Test
        @DisplayName("default config should have BM25 weight of 0.3")
        void defaultConfig_BM25Weight_Is0Point3() {
            // Arrange & Act
            RetrievalConfig config = new RetrievalConfig();

            // Assert
            assertThat(config.getBm25Weight()).isEqualTo(0.3f);
        }

        @Test
        @DisplayName("default config should have Vector weight of 0.7")
        void defaultConfig_VectorWeight_Is0Point7() {
            // Arrange & Act
            RetrievalConfig config = new RetrievalConfig();

            // Assert
            assertThat(config.getVectorWeight()).isEqualTo(0.7f);
        }

        @Test
        @DisplayName("default config should have chunk size of 512")
        void defaultConfig_ChunkSize_Is512() {
            // Arrange & Act
            RetrievalConfig config = new RetrievalConfig();

            // Assert
            assertThat(config.getChunkSize()).isEqualTo(512);
        }

        @Test
        @DisplayName("default config should have chunk overlap of 50")
        void defaultConfig_ChunkOverlap_Is50() {
            // Arrange & Act
            RetrievalConfig config = new RetrievalConfig();

            // Assert
            assertThat(config.getChunkOverlap()).isEqualTo(50);
        }

        @Test
        @DisplayName("default config should have embedding model all-MiniLM-L6-v2")
        void defaultConfig_EmbeddingModel_IsAllMiniLM() {
            // Arrange & Act
            RetrievalConfig config = new RetrievalConfig();

            // Assert
            assertThat(config.getEmbeddingModel()).isEqualTo("all-MiniLM-L6-v2");
        }

        @Test
        @DisplayName("default config weights should sum to 1.0")
        void defaultConfig_Weights_SumToOne() {
            // Arrange & Act
            RetrievalConfig config = new RetrievalConfig();
            float sum = config.getBm25Weight() + config.getVectorWeight();

            // Assert
            assertThat(sum).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("default config should validate successfully")
        void defaultConfig_Validate_DoesNotThrow() {
            // Arrange
            RetrievalConfig config = new RetrievalConfig();

            // Act & Assert
            assertThatNoException().isThrownBy(config::validate);
        }
    }

    @Nested
    @DisplayName("Configuration validation")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("should throw exception when weights don't sum to 1.0")
        void config_InvalidWeights_ThrowsException() {
            // Arrange
            RetrievalConfig invalidConfig = new RetrievalConfig();
            invalidConfig.setBm25Weight(0.5f);
            invalidConfig.setVectorWeight(0.6f); // Sum = 1.1, invalid

            // Act & Assert
            assertThatThrownBy(invalidConfig::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must sum to 1.0");
        }

        @Test
        @DisplayName("should accept valid weight configuration")
        void config_ValidWeights_ValidatesSuccessfully() {
            // Arrange
            RetrievalConfig validConfig = new RetrievalConfig();
            validConfig.setBm25Weight(0.4f);
            validConfig.setVectorWeight(0.6f); // Sum = 1.0, valid

            // Act & Assert
            assertThatNoException().isThrownBy(validConfig::validate);
        }

        @Test
        @DisplayName("should accept custom chunk sizes")
        void config_CustomChunkSizes_AcceptsValues() {
            // Arrange
            RetrievalConfig config = new RetrievalConfig();

            // Act
            config.setChunkSize(2048);
            config.setChunkOverlap(200);

            // Assert
            assertThat(config.getChunkSize()).isEqualTo(2048);
            assertThat(config.getChunkOverlap()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Embedding model configuration")
    class EmbeddingModelTests {

        @Test
        @DisplayName("should allow setting custom embedding model")
        void config_CustomEmbeddingModel_AcceptsValue() {
            // Arrange
            RetrievalConfig config = new RetrievalConfig();

            // Act
            config.setEmbeddingModel("custom-model");

            // Assert
            assertThat(config.getEmbeddingModel()).isEqualTo("custom-model");
        }

        @Test
        @DisplayName("should have default embedding dimension of 384")
        void config_DefaultEmbeddingDimension_Is384() {
            // Arrange & Act
            RetrievalConfig config = new RetrievalConfig();

            // Assert
            assertThat(config.getEmbeddingDimension()).isEqualTo(384);
        }
    }
}
