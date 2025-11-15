package com.mcp.server.ingest.factory;

import com.mcp.server.core.config.IngestConfig;
import com.mcp.server.ingest.api.*;
import com.mcp.server.ingest.chunker.RecursiveDocumentChunker;
import com.mcp.server.ingest.finder.MarkdownAndPdfFinder;
import com.mcp.server.ingest.indexer.LuceneBM25Indexer;
import com.mcp.server.ingest.loader.MultiFormatDocumentLoader;
import com.mcp.server.ingest.store.ChromaVectorStore;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default implementation of IngestionComponentFactory.
 *
 * Creates production-ready concrete implementations of all components:
 * - MarkdownAndPdfFinder for document discovery
 * - MultiFormatDocumentLoader with markdown and PDF parsers
 * - RecursiveDocumentChunker with configurable chunk size/overlap
 * - LuceneBM25Indexer for keyword search (persisted to /data/bm25_index)
 * - ChromaVectorStore for embedding storage
 */
public class DefaultIngestionComponentFactory implements IngestionComponentFactory {

    // Default BM25 index path (will be mounted as Docker volume)
    private static final Path DEFAULT_BM25_INDEX_PATH = Paths.get("/data/bm25_index");

    @Override
    public DocumentFinder createDocumentFinder() {
        return new MarkdownAndPdfFinder();
    }

    @Override
    public DocumentLoader createDocumentLoader() {
        return new MultiFormatDocumentLoader();
    }

    @Override
    public DocumentChunker createDocumentChunker(IngestConfig config) {
        return new RecursiveDocumentChunker(
            config.getChunkSize(),
            config.getChunkOverlap()
        );
    }

    @Override
    public KeywordIndexer createKeywordIndexer() {
        // Create BM25 indexer with persistent storage
        return new LuceneBM25Indexer(DEFAULT_BM25_INDEX_PATH);
    }

    @Override
    public VectorStore createVectorStore(String chromaHost, int chromaPort, String collectionName, IngestConfig config) {
        return new ChromaVectorStore(chromaHost, chromaPort, collectionName, config);
    }
}
