package com.mcp.server.ingest.store;

import com.mcp.server.core.config.IngestConfig;
import com.mcp.server.ingest.api.VectorStore;
import com.mcp.server.ingest.exception.VectorStoreException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ChromaDB-based vector store implementation.
 *
 * Handles document chunking, embedding generation, and storage in ChromaDB.
 * Uses all-MiniLM-L6-v2 embedding model (384 dimensions).
 */
public class ChromaVectorStore implements VectorStore {

    private static final Logger logger = LoggerFactory.getLogger(ChromaVectorStore.class);

    /**
     * ChromaDB has a maximum batch size of ~41,666 embeddings.
     * However, generating embeddings is slow, so we use a smaller batch size
     * of 500 to avoid timeouts. This provides a good balance between
     * throughput and reliability.
     */
    private static final int BATCH_SIZE = 500;

    private final String chromaHost;
    private final int chromaPort;
    private final String collectionName;
    private final IngestConfig config;

    private EmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingModel embeddingModel;

    /**
     * Constructor with configuration.
     *
     * @param chromaHost ChromaDB host
     * @param chromaPort ChromaDB port
     * @param collectionName Collection name
     * @param config Ingestion configuration
     */
    public ChromaVectorStore(String chromaHost, int chromaPort, String collectionName, IngestConfig config) {
        this.chromaHost = chromaHost;
        this.chromaPort = chromaPort;
        this.collectionName = collectionName;
        this.config = config;

        initializeComponents();
    }

    /**
     * Initialize embedding model, store, and document splitter.
     */
    private void initializeComponents() {
        logger.info("Initializing ChromaDB vector store...");

        try {
            // Create embedding model (all-MiniLM-L6-v2, same as Python)
            this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
            logger.info("Embedding model initialized: all-MiniLM-L6-v2");

            // Create ChromaDB embedding store
            String baseUrl = String.format("http://%s:%d", chromaHost, chromaPort);
            this.embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl(baseUrl)
                .collectionName(collectionName)
                .build();
            logger.info("ChromaDB store initialized: {}", baseUrl);
            logger.info("Ingestor pipeline initialized");

        } catch (Exception e) {
            throw new VectorStoreException("Failed to initialize ChromaDB vector store", e);
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        logger.info("-".repeat(60));
        logger.info("Adding {} pre-chunked documents to vector store (this may take a while)...", documents.size());

        try {
            processBatches(documents);
            logger.info("Successfully added {} chunks to vector store", documents.size());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to add documents to vector store", e);
        }
    }

    private void processBatches(List<Document> documents) {
        int totalChunks = documents.size();
        int batchCount = calculateBatchCount(totalChunks);

        logger.info("Processing {} chunks in {} batch(es)...", totalChunks, batchCount);

        for (int i = 0; i < totalChunks; i += BATCH_SIZE) {
            processBatch(documents, i, totalChunks);
        }
    }

    private int calculateBatchCount(int totalChunks) {
        return (int) Math.ceil((double) totalChunks / BATCH_SIZE);
    }

    private void processBatch(List<Document> documents, int startIndex, int totalChunks) {
        int end = Math.min(startIndex + BATCH_SIZE, totalChunks);
        List<Document> batch = documents.subList(startIndex, end);

        int batchNum = (startIndex / BATCH_SIZE) + 1;
        logger.info("Processing batch {}: {} chunks", batchNum, batch.size());

        EmbeddingStoreIngestor batchIngestor = createBatchIngestor();
        batchIngestor.ingest(batch);
    }

    private EmbeddingStoreIngestor createBatchIngestor() {
        return EmbeddingStoreIngestor.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();
    }

    @Override
    public int count() {
        // Note: ChromaEmbeddingStore in LangChain4j 0.36.2 doesn't expose count()
        // In production, you'd query ChromaDB directly for count
        logger.warn("Count operation not available via ChromaEmbeddingStore API");
        return 0; // Placeholder
    }

    @Override
    public void reset() {
        logger.info("WARNING: Re-ingestion requested - deleting collection...");

        try {
            // Note: ChromaEmbeddingStore doesn't expose delete() in LangChain4j 0.36.2
            // In production, you'd use ChromaDB client directly to delete collection
            logger.info("Collection deletion requested (would delete: {})", collectionName);

            // Reinitialize components after reset
            initializeComponents();

        } catch (Exception e) {
            throw new VectorStoreException("Failed to reset vector store", e);
        }
    }
}
