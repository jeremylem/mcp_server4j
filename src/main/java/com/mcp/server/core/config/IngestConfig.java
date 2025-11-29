package com.mcp.server.core.config;

public record IngestConfig(
        int chunkSize,
        int chunkOverlap,
        double bm25Weight,
        double vectorWeight,
        String collectionName,
        String chromaHost,
        int chromaPort
) {
    public IngestConfig() {
        this(512, 50, 0.3, 0.7, "baseline_kb", "chroma", 8000);
    }

    public IngestConfig(int chunkSize, int chunkOverlap, double bm25Weight,
                        double vectorWeight, String collectionName) {
        this(chunkSize, chunkOverlap, bm25Weight, vectorWeight, collectionName, "chroma", 8000);
    }
}
