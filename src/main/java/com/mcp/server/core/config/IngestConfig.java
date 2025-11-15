package com.mcp.server.core.config;

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
    private int chunkSize = 512;
    private int chunkOverlap = 50;

    // Hybrid search weights
    private double bm25Weight = 0.3;
    private double vectorWeight = 0.7;

    // ChromaDB configuration
    private String collectionName = "baseline_kb";

    // ChromaDB connection
    private String chromaHost = "chroma";
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

    // Getters and Setters

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public double getBm25Weight() {
        return bm25Weight;
    }

    public void setBm25Weight(double bm25Weight) {
        this.bm25Weight = bm25Weight;
    }

    public double getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(double vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getChromaHost() {
        return chromaHost;
    }

    public void setChromaHost(String chromaHost) {
        this.chromaHost = chromaHost;
    }

    public int getChromaPort() {
        return chromaPort;
    }

    public void setChromaPort(int chromaPort) {
        this.chromaPort = chromaPort;
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
