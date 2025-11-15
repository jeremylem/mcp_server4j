package com.mcp.server.ingest.api;

import com.mcp.server.core.models.IngestionRequest;
import com.mcp.server.core.models.IngestionResult;
import com.mcp.server.ingest.exception.IngestionException;

/**
 * Orchestrates the complete document ingestion pipeline.
 *
 * The pipeline typically includes:
 * 1. Finding documents
 * 2. Loading documents
 * 3. Chunking documents
 * 4. Building keyword index
 * 5. Storing embeddings in vector store
 * 6. Verifying ingestion
 */
public interface IngestionPipeline {

    /**
     * Execute the ingestion pipeline.
     *
     * @param request Ingestion parameters
     * @return Ingestion statistics
     * @throws IngestionException if any step of the pipeline fails
     */
    IngestionResult ingest(IngestionRequest request);
}
