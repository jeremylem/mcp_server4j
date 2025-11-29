package com.mcp.server.ingest.factory;

import com.mcp.server.core.config.IngestConfig;
import com.mcp.server.ingest.api.*;

public interface IngestionComponentFactory {

    DocumentFinder createDocumentFinder();

    DocumentLoader createDocumentLoader();

    DocumentChunker createDocumentChunker(IngestConfig config);

    KeywordIndexer createKeywordIndexer();

    VectorStore createVectorStore(String chromaHost, int chromaPort, String collectionName, IngestConfig config);
}
