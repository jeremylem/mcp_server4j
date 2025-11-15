package com.mcp.server.mcp;

import com.mcp.server.core.config.RetrievalConfig;
import com.mcp.server.core.interfaces.Retriever;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for MCP Server integration tests.
 *
 * Provides in-memory implementations and test doubles for:
 * - Retriever (BaselineRetriever)
 * - ChromaDB (in-memory test store)
 * - BM25 indexer
 * - Document chunker
 */
@TestConfiguration
public class McpServerTestConfig {

    @Bean
    @Primary
    public Retriever testRetriever() {
        // Create test configuration
        RetrievalConfig config = new RetrievalConfig();
        config.setBm25Weight(0.3f);
        config.setVectorWeight(0.7f);
        config.setCandidatePoolSize(20);

        // Create in-memory embedding model for testing
        EmbeddingModel embeddingModel = new TestEmbeddingModel();

        // Create in-memory ChromaDB store
        ChromaEmbeddingStore embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl("http://localhost:8000")
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
     * Simple test embedding model that returns fixed-size vectors.
     * For real testing, you'd want to use the actual embedding model.
     */
    private static class TestEmbeddingModel implements EmbeddingModel {
        @Override
        public Response<Embedding> embed(String text) {
            // Return a simple fixed-size embedding for testing
            float[] vector = new float[384]; // all-MiniLM-L6-v2 size
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) Math.random();
            }
            return Response.from(Embedding.from(vector));
        }

        @Override
        public Response<Embedding> embed(TextSegment textSegment) {
            return embed(textSegment.text());
        }

        @Override
        public Response<java.util.List<Embedding>> embedAll(java.util.List<TextSegment> textSegments) {
            java.util.List<Embedding> embeddings = new java.util.ArrayList<>();
            for (TextSegment segment : textSegments) {
                embeddings.add(embed(segment).content());
            }
            return Response.from(embeddings);
        }
    }
}
