package com.mcp.server.retrieval;

import com.mcp.server.core.config.RetrievalConfig;
import com.mcp.server.core.models.SearchResult;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid search retriever (BM25 + Vector).
 * Orchestrates search and delegates fusion to HybridScoreFusion.
 */
public class BaselineRetriever
        implements
            com.mcp.server.core.interfaces.QueryService,
            com.mcp.server.core.interfaces.DocumentManager,
            com.mcp.server.core.interfaces.Initializable,
            com.mcp.server.core.interfaces.DocumentChunker {

    private static final Logger logger = LoggerFactory.getLogger(BaselineRetriever.class);

    private final ChromaVectorSearch vectorSearch;
    private final com.mcp.server.ingest.indexer.LuceneBM25Indexer bm25Indexer;
    private final com.mcp.server.ingest.api.DocumentChunker documentChunker;
    private final RetrievalConfig config;
    private final HybridScoreFusion scoreFusion;

    public BaselineRetriever(
            ChromaVectorSearch vectorSearch,
            com.mcp.server.ingest.indexer.LuceneBM25Indexer bm25Indexer,
            com.mcp.server.ingest.api.DocumentChunker documentChunker,
            RetrievalConfig config
    ) {
        this.vectorSearch = vectorSearch;
        this.bm25Indexer = bm25Indexer;
        this.documentChunker = documentChunker;
        this.config = config;
        this.scoreFusion = new HybridScoreFusion(config);

        config.validate();
        logger.info("BaselineRetriever initialized (BM25={}, Vector={})",
                config.getBm25Weight(), config.getVectorWeight());
    }

    @Override
    public void initialize() {
        logger.info("Loading persisted BM25 index...");

        try {
            if (bm25Indexer instanceof com.mcp.server.ingest.indexer.LuceneBM25Indexer) {
                com.mcp.server.ingest.indexer.LuceneBM25Indexer luceneIndexer =
                    (com.mcp.server.ingest.indexer.LuceneBM25Indexer) bm25Indexer;

                if (luceneIndexer.indexExistsOnDisk()) {
                    luceneIndexer.loadIndex();
                    logger.info("BM25 index loaded");
                } else {
                    logger.warn("No BM25 index found. Run ingestion first.");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize: {}", e.getMessage(), e);
            throw new RuntimeException("Initialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> query(String query, int topK, boolean useHybrid, String filterType) {
        logger.info("Query: '{}' (hybrid={}, topK={}, filter={})", query, useHybrid, topK, filterType);

        if (query == null || query.trim().isEmpty()) {
            logger.warn("Empty query");
            return new ArrayList<>();
        }

        Map<String, String> filterMetadata = null;
        if (filterType != null && !filterType.isEmpty()) {
            filterMetadata = new HashMap<>();
            filterMetadata.put("type", filterType);
        }

        return useHybrid ? hybridSearch(query, topK, filterMetadata) : vectorOnlySearch(query, topK, filterMetadata);
    }

    private List<Map<String, Object>> hybridSearch(String query, int topK, Map<String, String> filterMetadata) {
        int candidatePoolSize = config.getCandidatePoolSize();

        List<SearchResult> bm25Results = bm25Indexer.search(query, candidatePoolSize);
        List<VectorSearchResult> vectorResults = vectorSearch.search(query, candidatePoolSize, filterMetadata);
        List<Map<String, Object>> results = scoreFusion.fuse(bm25Results, vectorResults, topK);

        logger.info("Hybrid search: {} results", results.size());
        return results;
    }

    private List<Map<String, Object>> vectorOnlySearch(String query, int topK, Map<String, String> filterMetadata) {
        List<VectorSearchResult> vectorResults = vectorSearch.search(query, topK, filterMetadata);

        List<Map<String, Object>> results = new ArrayList<>();
        for (VectorSearchResult result : vectorResults) {
            Document doc = result.getDocument();
            double confidence = 1.0 / (1.0 + result.getDistance());

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("content", doc.text());
            resultMap.put("metadata", ChromaVectorSearch.metadataToMap(doc.metadata()));
            resultMap.put("confidence", confidence);
            results.add(resultMap);
        }

        logger.info("Vector search: {} results", results.size());
        return results;
    }

    @Override
    public void addDocuments(List<Document> documents) {
        logger.info("Adding {} documents", documents.size());

        try {
            vectorSearch.addDocuments(documents);
            List<Document> allDocuments = vectorSearch.getAllDocuments();
            bm25Indexer.buildIndex(allDocuments);
            logger.info("Added {} documents", documents.size());
        } catch (Exception e) {
            logger.error("Failed to add documents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add documents: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> chunkDocuments(List<Document> documents) {
        return documentChunker.chunkDocuments(documents);
    }
}
