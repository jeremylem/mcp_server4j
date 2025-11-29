package com.mcp.server.core.models;

/**
 * Result returned from document ingestion.
 */
public record IngestionResult(
        int documentsProcessed,
        int chunksCreated,
        int totalInCollection,
        String collectionName
) {
}
