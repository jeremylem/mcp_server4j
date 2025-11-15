package com.mcp.server.core.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for retrieval system.
 *
 * Contains settings for:
 * - Embedding model
 * - Hybrid search weights
 * - Chunking parameters
 */
@Getter
@Setter
public class RetrievalConfig {

    // Embedding Model
    private String embeddingModel = "all-MiniLM-L6-v2";
    private int embeddingDimension = 384;

    // Hybrid Search Weights
    private float bm25Weight = 0.3f;  // Weight for BM25 keyword search
    private float vectorWeight = 0.7f; // Weight for vector semantic search

    // Chunking Parameters
    private int chunkSize = 512;
    private int chunkOverlap = 50;

    // Search Parameters
    private int defaultTopK = 5;
    private int candidatePoolSize = 20; // Number of candidates from each search method

    public RetrievalConfig() {
        // Default constructor with preset values
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
