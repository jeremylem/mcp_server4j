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
 * Document ingestion pipeline orchestrator.
 */
public class DocumentIngestionPipeline implements IngestionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionPipeline.class);

    private final DocumentFinder documentFinder;
    private final DocumentLoader documentLoader;
    private final DocumentChunker documentChunker;
    private final KeywordIndexer keywordIndexer;  // For BM25 index building
    private final VectorStore vectorStore;

    private DocumentIngestionPipeline(Builder builder) {
        this.documentFinder = builder.documentFinder;
        this.documentLoader = builder.documentLoader;
        this.documentChunker = builder.documentChunker;
        this.keywordIndexer = builder.keywordIndexer;
        this.vectorStore = builder.vectorStore;
    }

    @Override
    public IngestionResult ingest(IngestionRequest request) {
        logger.info("Starting ingestion: {} (re-ingest={})", request.getDocsDir(), request.isReIngest());

        try {
            if (request.isReIngest()) {
                vectorStore.reset();
            }

            List<Path> paths = documentFinder.findDocuments(request.getDocsDir());
            if (paths.isEmpty()) {
                throw new IngestionException("No documents found in: " + request.getDocsDir());
            }

            List<Document> documents = documentLoader.loadDocuments(paths);
            if (documents.isEmpty()) {
                throw new IngestionException("No documents loaded");
            }

            List<Document> chunks = documentChunker.chunkDocuments(documents);

            logger.info("Building BM25 index...");
            keywordIndexer.buildIndex(chunks);
            logger.info("BM25 index built");

            vectorStore.addDocuments(chunks);

            int totalChunks = vectorStore.count();
            logger.info("Ingestion complete: {} docs, {} chunks, {} total",
                documents.size(), chunks.size(), totalChunks);

            return new IngestionResult(
                documents.size(),
                chunks.size(),
                totalChunks,
                request.getCollectionName()
            );

        } catch (IngestionException e) {
            throw e;
        } catch (Exception e) {
            throw new IngestionException("Ingestion failed", e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

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
            if (documentFinder == null) throw new IllegalStateException("DocumentFinder required");
            if (documentLoader == null) throw new IllegalStateException("DocumentLoader required");
            if (documentChunker == null) throw new IllegalStateException("DocumentChunker required");
            if (keywordIndexer == null) throw new IllegalStateException("KeywordIndexer required");
            if (vectorStore == null) throw new IllegalStateException("VectorStore required");

            return new DocumentIngestionPipeline(this);
        }
    }
}
