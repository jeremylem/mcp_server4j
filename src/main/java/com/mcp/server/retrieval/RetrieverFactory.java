package com.mcp.server.retrieval;

import com.mcp.server.core.config.RetrievalConfig;
import com.mcp.server.core.interfaces.DocumentManager;
import com.mcp.server.core.interfaces.QueryService;
import com.mcp.server.core.interfaces.Retriever;
import com.mcp.server.ingest.api.DocumentChunker;
import com.mcp.server.ingest.chunker.RecursiveDocumentChunker;
import com.mcp.server.ingest.indexer.LuceneBM25Indexer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Factory for creating BaselineRetriever instances with all dependencies.
 *
 * Handles dependency creation and wiring following the Factory pattern.
 * Makes it easy to create retrievers for different environments (dev, test, prod).
 */
public class RetrieverFactory {

    private static final Logger logger = LoggerFactory.getLogger(RetrieverFactory.class);

    // BM25 index path (must match ingestion pipeline)
    private static final Path BM25_INDEX_PATH = Paths.get("/data/bm25_index");

    /**
     * Create a QueryService with default configuration.
     * Preferred method for components that only need query capabilities.
     *
     * @param chromaHost ChromaDB host
     * @param chromaPort ChromaDB port
     * @param collectionName Collection name
     * @return Configured QueryService
     */
    public static QueryService createQueryService(
            String chromaHost,
            int chromaPort,
            String collectionName
    ) {
        return createBaselineRetriever(chromaHost, chromaPort, collectionName, new RetrievalConfig());
    }

    /**
     * Create a QueryService with custom configuration.
     * Preferred method for components that only need query capabilities.
     *
     * @param chromaHost ChromaDB host
     * @param chromaPort ChromaDB port
     * @param collectionName Collection name
     * @param config Retrieval configuration
     * @return Configured QueryService
     */
    public static QueryService createQueryService(
            String chromaHost,
            int chromaPort,
            String collectionName,
            RetrievalConfig config
    ) {
        return createBaselineRetriever(chromaHost, chromaPort, collectionName, config);
    }

    /**
     * Create a DocumentManager with default configuration.
     * Preferred method for components that only need document management capabilities.
     *
     * @param chromaHost ChromaDB host
     * @param chromaPort ChromaDB port
     * @param collectionName Collection name
     * @return Configured DocumentManager
     */
    public static DocumentManager createDocumentManager(
            String chromaHost,
            int chromaPort,
            String collectionName
    ) {
        return createBaselineRetriever(chromaHost, chromaPort, collectionName, new RetrievalConfig());
    }

    /**
     * Create a BaselineRetriever with default configuration.
     *
     * @param chromaHost ChromaDB host
     * @param chromaPort ChromaDB port
     * @param collectionName Collection name
     * @return Configured BaselineRetriever
     * @deprecated Use createQueryService() or createDocumentManager() for focused interfaces
     */
    @Deprecated
    public static Retriever createBaselineRetriever(
            String chromaHost,
            int chromaPort,
            String collectionName
    ) {
        return createBaselineRetriever(chromaHost, chromaPort, collectionName, new RetrievalConfig());
    }

    /**
     * Create a BaselineRetriever with custom configuration.
     *
     * @param chromaHost ChromaDB host
     * @param chromaPort ChromaDB port
     * @param collectionName Collection name
     * @param config Retrieval configuration
     * @return Configured BaselineRetriever
     * @deprecated Use createQueryService() or createDocumentManager() for focused interfaces
     */
    @Deprecated
    public static Retriever createBaselineRetriever(
            String chromaHost,
            int chromaPort,
            String collectionName,
            RetrievalConfig config
    ) {
        logger.info("Creating BaselineRetriever...");
        logger.info("  ChromaDB: {}:{}", chromaHost, chromaPort);
        logger.info("  Collection: {}", collectionName);
        logger.info("  Embedding Model: {}", config.getEmbeddingModel());
        logger.info("  BM25 Weight: {}, Vector Weight: {}", config.getBm25Weight(), config.getVectorWeight());

        // 1. Create Embedding Model
        EmbeddingModel embeddingModel = createEmbeddingModel(config);

        // 2. Create ChromaDB Embedding Store
        EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl(String.format("http://%s:%d", chromaHost, chromaPort))
                .collectionName(collectionName)
                .timeout(Duration.ofSeconds(30))
                .build();

        logger.info("ChromaDB embedding store created");

        // 3. Create Vector Search component
        ChromaVectorSearch vectorSearch = new ChromaVectorSearch(embeddingStore, embeddingModel);

        // 4. Create BM25 Indexer (with persistent storage)
        LuceneBM25Indexer bm25Indexer = new LuceneBM25Indexer(BM25_INDEX_PATH);
        logger.info("BM25 indexer configured with persistent storage: {}", BM25_INDEX_PATH);

        // 5. Create Document Chunker
        DocumentChunker documentChunker = new RecursiveDocumentChunker(
                config.getChunkSize(),
                config.getChunkOverlap()
        );

        // 6. Create and return BaselineRetriever
        BaselineRetriever retriever = new BaselineRetriever(
                vectorSearch,
                bm25Indexer,
                documentChunker,
                config
        );

        logger.info("BaselineRetriever created successfully");
        return retriever;
    }

    /**
     * Create embedding model based on configuration.
     *
     * @param config Retrieval configuration
     * @return Embedding model
     */
    private static EmbeddingModel createEmbeddingModel(RetrievalConfig config) {
        String modelName = config.getEmbeddingModel();

        logger.info("Creating embedding model: {}", modelName);

        if (modelName.equals("all-MiniLM-L6-v2") || modelName.equals("AllMiniLmL6V2")) {
            // Use ONNX-based all-MiniLM-L6-v2 (local, no API calls)
            return new AllMiniLmL6V2EmbeddingModel();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported embedding model: " + modelName + ". Only 'all-MiniLM-L6-v2' is currently supported."
            );
        }
    }

    /**
     * Create a test retriever with mock components (for testing).
     *
     * @return Test retriever
     */
    public static Retriever createTestRetriever() {
        logger.info("Creating test retriever with mock components");
        // This would create a retriever with mock/in-memory components
        // Useful for unit testing
        throw new UnsupportedOperationException("Test retriever not yet implemented");
    }
}
