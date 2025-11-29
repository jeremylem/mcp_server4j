package com.mcp.server.core.config;

/**
 * Configuration for retrieval system.
 * Contains settings for embedding model, hybrid search weights, and chunking parameters.
 */
public class RetrievalConfig {

    // Embedding Model
    private String embeddingModel = "all-MiniLM-L6-v2";

    // Hybrid Search Weights
    private float bm25Weight = 0.3f;  // Weight for BM25 keyword search
    private float vectorWeight = 0.7f; // Weight for vector semantic search

    // Chunking Parameters
    private int chunkSize = 512;
    private int chunkOverlap = 50;

    // Search Parameters
    private int candidatePoolSize = 20; // Number of candidates from each search method

    public RetrievalConfig() {
        // Default constructor with preset values
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float getBm25Weight() {
        return bm25Weight;
    }

    public void setBm25Weight(float bm25Weight) {
        this.bm25Weight = bm25Weight;
    }

    public float getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(float vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

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

    public int getCandidatePoolSize() {
        return candidatePoolSize;
    }

    public void setCandidatePoolSize(int candidatePoolSize) {
        this.candidatePoolSize = candidatePoolSize;
    }

    /**
     * Validate that weights sum to 1.0.
     */
    public void validate() {
        float sum = bm25Weight + vectorWeight;
        if (Math.abs(sum - 1.0f) > 0.001f) {
            throw new IllegalStateException(
                    String.format("BM25 and Vector weights must sum to 1.0 (got %.3f)", sum)
            );
        }
    }
}
