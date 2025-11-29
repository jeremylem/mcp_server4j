package com.mcp.server.mcp;

import com.mcp.server.core.config.RetrievalConfig;
import com.mcp.server.ingest.api.DocumentChunker;
import com.mcp.server.ingest.chunker.RecursiveDocumentChunker;
import com.mcp.server.ingest.indexer.LuceneBM25Indexer;
import com.mcp.server.retrieval.BaselineRetriever;
import com.mcp.server.retrieval.ChromaVectorSearch;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Test configuration for MCP Server integration tests.
 * <p>
 * Provides test implementations for:
 * - Retriever (BaselineRetriever)
 * - ChromaDB (connected via TestContainers)
 * - Test embedding model (fast random vectors)
 * - BM25 indexer (in-memory)
 * - Document chunker
 * <p>
 * Uses @DynamicPropertySource from the test to connect to TestContainers ChromaDB.
 */
@TestConfiguration
public class McpServerTestConfig {

    @Value("${chroma.host:localhost}")
    private String chromaHost;

    @Value("${chroma.port:8000}")
    private int chromaPort;

    @Bean("baselineRetriever")
    public BaselineRetriever baselineRetriever() {
        // Create test configuration
        RetrievalConfig config = new RetrievalConfig();
        config.setBm25Weight(0.3f);
        config.setVectorWeight(0.7f);
        config.setCandidatePoolSize(20);

        // Create test embedding model (uses random vectors for fast testing)
        EmbeddingModel embeddingModel = new TestEmbeddingModel();

        // Create ChromaDB store connected to TestContainers instance
        String baseUrl = String.format("http://%s:%d", chromaHost, chromaPort);
        ChromaEmbeddingStore embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl(baseUrl)
                .collectionName("test_kb")
                .build();

        // Create vector search
        ChromaVectorSearch vectorSearch = new ChromaVectorSearch(
                embeddingStore,
                embeddingModel
        );

        // Create BM25 indexer
        LuceneBM25Indexer bm25Indexer = new LuceneBM25Indexer();

        // Create chunker
        DocumentChunker chunker = new RecursiveDocumentChunker(512, 50);

        // Create retriever
        return new BaselineRetriever(vectorSearch, bm25Indexer, chunker, config);
    }

    /**
     * Override the queryService bean from main configuration to use test retriever.
     */
    @Bean("queryService")
    @Primary
    public com.mcp.server.core.interfaces.QueryService queryService() {
        return baselineRetriever();
    }

    /**
     * Override the documentManager bean from main configuration to use test retriever.
     */
    @Bean("documentManager")
    public com.mcp.server.core.interfaces.DocumentManager documentManager() {
        return baselineRetriever();
    }

    /**
     * Simple test embedding model that returns deterministic fixed-size vectors.
     * Uses text hash to ensure consistent vectors for the same text.
     */
    private static class TestEmbeddingModel implements EmbeddingModel {
        @Override
        public Response<@NotNull Embedding> embed(String text) {
            // Return deterministic embedding based on text hash
            // This ensures same text always gets same vector (critical for search to work!)
            float[] vector = new float[384]; // all-MiniLM-L6-v2 size

            // Use text hash as seed for deterministic random generation
            int hash = text.hashCode();
            java.util.Random random = new java.util.Random(hash);

            for (int i = 0; i < vector.length; i++) {
                vector[i] = random.nextFloat();
            }

            // Normalize vector (important for cosine similarity)
            float norm = 0;
            for (float v : vector) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }

            return Response.from(Embedding.from(vector));
        }

        @Override
        public Response<@NotNull Embedding> embed(TextSegment textSegment) {
            return embed(textSegment.text());
        }

        @Override
        public Response<@NotNull List<Embedding>> embedAll(java.util.List<TextSegment> textSegments) {
            java.util.List<Embedding> embeddings = new java.util.ArrayList<>();
            for (TextSegment segment : textSegments) {
                embeddings.add(embed(segment).content());
            }
            return Response.from(embeddings);
        }
    }
}
