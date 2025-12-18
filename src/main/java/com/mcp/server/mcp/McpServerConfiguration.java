package com.mcp.server.mcp;

import com.mcp.server.core.config.RetrievalConfig;
import com.mcp.server.core.interfaces.DocumentManager;
import com.mcp.server.core.interfaces.QueryService;
import com.mcp.server.ingest.api.DocumentChunker;
import com.mcp.server.ingest.chunker.RecursiveDocumentChunker;
import com.mcp.server.ingest.indexer.LuceneBM25Indexer;
import com.mcp.server.retrieval.BaselineRetriever;
import com.mcp.server.retrieval.ChromaVectorSearch;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring configuration for the RAG system:
 * ChromaDB, embedding model, BM25 indexer, and retriever.
 */
@Configuration
@Profile("!test")
public class McpServerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpServerConfiguration.class);

    @Value("${chroma.host:localhost}")
    private String chromaHost;

    @Value("${chroma.port:8000}")
    private int chromaPort;

    @Value("${chroma.collection-name:baseline_kb}")
    private String collectionName;

    @Value("${retrieval.bm25-weight:0.3}")
    private float bm25Weight;

    @Value("${retrieval.vector-weight:0.7}")
    private float vectorWeight;

    @Value("${retrieval.candidate-pool-size:20}")
    private int candidatePoolSize;

    @Bean
    public EmbeddingModel embeddingModel() {
        logger.info("Creating AllMiniLmL6V2EmbeddingModel...");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public ChromaEmbeddingStore chromaEmbeddingStore() {
        String baseUrl = String.format("http://%s:%d", chromaHost, chromaPort);
        logger.info("Creating ChromaEmbeddingStore: {}, collection: {}", baseUrl, collectionName);

        return ChromaEmbeddingStore.builder()
                .baseUrl(baseUrl)
                .collectionName(collectionName)
                .build();
    }

    @Bean
    public ChromaVectorSearch chromaVectorSearch(
            ChromaEmbeddingStore embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        logger.info("Creating ChromaVectorSearch...");
        return new ChromaVectorSearch(embeddingStore, embeddingModel);
    }

    @Bean
    public LuceneBM25Indexer bm25Indexer() {
        // BM25 index path (must match ingestion pipeline)
        Path bm25IndexPath = Paths.get("/data/bm25_index");
        logger.info("Creating LuceneBM25Indexer with persistent storage: {}", bm25IndexPath);
        return new LuceneBM25Indexer(bm25IndexPath);
    }

    @Bean
    public DocumentChunker documentChunker() {
        logger.info("Creating RecursiveDocumentChunker...");
        return new RecursiveDocumentChunker(512, 50);
    }

    @Bean
    public RetrievalConfig retrievalConfig() {
        logger.info("Creating RetrievalConfig: BM25={}, Vector={}, pool={}",
                bm25Weight, vectorWeight, candidatePoolSize);

        RetrievalConfig config = new RetrievalConfig();
        config.setBm25Weight(bm25Weight);
        config.setVectorWeight(vectorWeight);
        config.setCandidatePoolSize(candidatePoolSize);
        config.validate();

        return config;
    }

    /**
     * Create the BaselineRetriever bean.
     * Implements Retriever (and thus QueryService, DocumentManager, Initializable).
     */
    @Bean
    public BaselineRetriever baselineRetriever(
            ChromaVectorSearch vectorSearch,
            LuceneBM25Indexer bm25Indexer,
            DocumentChunker chunker,
            RetrievalConfig config
    ) {
        logger.info("Creating BaselineRetriever...");

        BaselineRetriever retriever = new BaselineRetriever(
                vectorSearch,
                bm25Indexer,
                chunker,
                config
        );

        // Note: initialize() is not called here to avoid issues in tests
        // The application should call initialize() explicitly when needed
        logger.info("BaselineRetriever bean created (call initialize() to load persisted index)");

        return retriever;
    }

    /**
     * Provide QueryService interface (satisfied by BaselineRetriever).
     * This is the preferred interface for components that only need query capabilities.
     * Marked as @Primary to resolve ambiguity when multiple QueryService beans exist.
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public QueryService queryService(BaselineRetriever retriever) {
        return retriever;
    }

    /**
     * Provide DocumentManager interface (satisfied by BaselineRetriever).
     * Use this interface for components that only need document management capabilities.
     */
    @Bean
    public DocumentManager documentManager(BaselineRetriever baselineRetriever) {
        return baselineRetriever;
    }

    /**
     * Provide DocumentChunker interface (satisfied by BaselineRetriever).
     * Use this interface for components that only need document chunking capabilities.
     */
    @Bean
    public com.mcp.server.core.interfaces.DocumentChunker coreDocumentChunker(BaselineRetriever baselineRetriever) {
        return baselineRetriever;
    }

    /**
     * Initialize the retriever on application startup.
     * Loads the persisted BM25 index from disk.
     */
    @Bean
    public org.springframework.boot.ApplicationRunner initializeRetriever(BaselineRetriever retriever) {
        return args -> {
            logger.info("Initializing BaselineRetriever (loading BM25 index from disk)...");
            try {
                retriever.initialize();
                logger.info("BaselineRetriever initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize BaselineRetriever: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize retriever", e);
            }
        };
    }

}
