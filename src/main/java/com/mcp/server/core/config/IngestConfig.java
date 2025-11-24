package com.mcp.server.core.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration constants for document ingestion.
 *
 * Mirrors the Python configuration from the original ingest.py:
 * - CHUNK_SIZE = 512 (characters)
 * - CHUNK_OVERLAP = 50 (characters)
 * - BM25_WEIGHT = 0.3
 * - VECTOR_WEIGHT = 0.7
 * - COLLECTION_NAME = "baseline_kb"
 */
public class IngestConfig {

    // Chunking configuration
    @Getter
    private int chunkSize = 512;
    @Getter
    private int chunkOverlap = 50;

    // Hybrid search weights
    private double bm25Weight = 0.3;
    private double vectorWeight = 0.7;

    // ChromaDB configuration
    @Setter
    @Getter
    private String collectionName = "baseline_kb";

    // ChromaDB connection
    @Setter
    @Getter
    private String chromaHost = "chroma";
    @Setter
    @Getter
    private int chromaPort = 8000;

    /**
     * Default constructor with standard configuration.
     */
    public IngestConfig() {
        // Use default values
    }

    /**
     * Constructor with custom configuration.
     */
    public IngestConfig(int chunkSize, int chunkOverlap, double bm25Weight,
                       double vectorWeight, String collectionName) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.bm25Weight = bm25Weight;
        this.vectorWeight = vectorWeight;
        this.collectionName = collectionName;
    }

    @Override
    public String toString() {
        return "IngestConfig{" +
                "chunkSize=" + chunkSize +
                ", chunkOverlap=" + chunkOverlap +
                ", bm25Weight=" + bm25Weight +
                ", vectorWeight=" + vectorWeight +
                ", collectionName='" + collectionName + '\'' +
                ", chromaHost='" + chromaHost + '\'' +
                ", chromaPort=" + chromaPort +
                '}';
    }
}
