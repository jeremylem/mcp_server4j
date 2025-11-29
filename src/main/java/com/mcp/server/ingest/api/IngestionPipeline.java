package com.mcp.server.ingest.api;

import com.mcp.server.core.models.IngestionRequest;
import com.mcp.server.core.models.IngestionResult;

/**
 * Orchestrates the complete document ingestion pipeline.
 * <p>
 * The pipeline typically includes:
 * 1. Finding documents
 * 2. Loading documents
 * 3. Chunking documents
 * 4. Building keyword index
 * 5. Storing embeddings in vector store
 * 6. Verifying ingestion
 */
public interface IngestionPipeline {

    IngestionResult ingest(IngestionRequest request);
}
