package com.mcp.server.ingest.pipeline;

import com.mcp.server.core.models.IngestionRequest;
import com.mcp.server.core.models.IngestionResult;
import com.mcp.server.ingest.api.*;
import com.mcp.server.ingest.exception.IngestionException;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates the complete document ingestion pipeline.
 *
 * This is the main pipeline implementation that follows SOLID principles:
 * - Single Responsibility: Only orchestrates the pipeline steps
 * - Open/Closed: Open for extension via interface implementations
 * - Liskov Substitution: Works with any interface implementations
 * - Interface Segregation: Depends on focused interfaces
 * - Dependency Inversion: Depends on abstractions, not concrete classes
 */
public class DocumentIngestionPipeline implements IngestionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionPipeline.class);

    private final DocumentFinder documentFinder;
    private final DocumentLoader documentLoader;
    private final DocumentChunker documentChunker;
    private final KeywordIndexer keywordIndexer;  // For BM25 index building
    private final VectorStore vectorStore;

    /**
     * Private constructor - use Builder to create instances.
     */
    private DocumentIngestionPipeline(Builder builder) {
        this.documentFinder = builder.documentFinder;
        this.documentLoader = builder.documentLoader;
        this.documentChunker = builder.documentChunker;
        this.keywordIndexer = builder.keywordIndexer;
        this.vectorStore = builder.vectorStore;
    }

    @Override
    public IngestionResult ingest(IngestionRequest request) {
        logger.info("=".repeat(60));
        logger.info("Document Ingestion - Baseline + Hybrid Search");
        logger.info("=".repeat(60));
        logger.info("Documents directory: {}", request.getDocsDir());
        logger.info("Re-ingest mode: {}", request.isReIngest());
        logger.info("=".repeat(60));

        try {
            // Step 1: Handle re-ingestion
            if (request.isReIngest()) {
                vectorStore.reset();
            }

            // Step 2: Find documents
            List<Path> paths = documentFinder.findDocuments(request.getDocsDir());

            if (paths.isEmpty()) {
                throw new IngestionException("No documents found to ingest in: " + request.getDocsDir());
            }

            // Step 3: Load documents
            List<Document> documents = documentLoader.loadDocuments(paths);

            if (documents.isEmpty()) {
                throw new IngestionException("No documents were successfully loaded!");
            }

            // Step 4: Chunk documents
            List<Document> chunks = documentChunker.chunkDocuments(documents);

            // Step 5: Build BM25 index (persisted to disk)
            logger.info("-".repeat(60));
            logger.info("Building BM25 index for keyword search...");
            keywordIndexer.buildIndex(chunks);
            logger.info("BM25 index built and persisted");

            // Step 6: Add chunks to vector store
            vectorStore.addDocuments(chunks);

            // Step 7: Verify ingestion
            logger.info("-".repeat(60));
            logger.info("Verifying ingestion...");
            int totalChunks = vectorStore.count();
            logger.info("Verification complete");

            // Summary
            logger.info("=".repeat(60));
            logger.info("INGESTION COMPLETE");
            logger.info("=".repeat(60));
            logger.info("Documents processed: {}", documents.size());
            logger.info("Chunks created: {}", chunks.size());
            logger.info("Total in collection: {}", totalChunks);
            logger.info("Collection: {}", request.getCollectionName());
            logger.info("=".repeat(60));

            return new IngestionResult(
                documents.size(),
                chunks.size(),
                totalChunks,
                request.getCollectionName()
            );

        } catch (IngestionException e) {
            throw e;
        } catch (Exception e) {
            throw new IngestionException("Ingestion pipeline failed", e);
        }
    }

    /**
     * Create a builder for constructing DocumentIngestionPipeline instances.
     *
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DocumentIngestionPipeline.
     *
     * Enforces that all required components are provided.
     */
    public static class Builder {
        private DocumentFinder documentFinder;
        private DocumentLoader documentLoader;
        private DocumentChunker documentChunker;
        private KeywordIndexer keywordIndexer;
        private VectorStore vectorStore;

        private Builder() {
        }

        public Builder withDocumentFinder(DocumentFinder documentFinder) {
            this.documentFinder = documentFinder;
            return this;
        }

        public Builder withDocumentLoader(DocumentLoader documentLoader) {
            this.documentLoader = documentLoader;
            return this;
        }

        public Builder withDocumentChunker(DocumentChunker documentChunker) {
            this.documentChunker = documentChunker;
            return this;
        }

        public Builder withKeywordIndexer(KeywordIndexer keywordIndexer) {
            this.keywordIndexer = keywordIndexer;
            return this;
        }

        public Builder withVectorStore(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
            return this;
        }

        public DocumentIngestionPipeline build() {
            // Validate all components are provided
            if (documentFinder == null) {
                throw new IllegalStateException("DocumentFinder is required");
            }
            if (documentLoader == null) {
                throw new IllegalStateException("DocumentLoader is required");
            }
            if (documentChunker == null) {
                throw new IllegalStateException("DocumentChunker is required");
            }
            if (keywordIndexer == null) {
                throw new IllegalStateException("KeywordIndexer is required");
            }
            if (vectorStore == null) {
                throw new IllegalStateException("VectorStore is required");
            }

            return new DocumentIngestionPipeline(this);
        }
    }
}
