package com.mcp.server.retrieval;

import com.mcp.server.core.config.RetrievalConfig;
import com.mcp.server.core.models.SearchResult;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Performs hybrid score fusion combining BM25 and vector search results.
 *
 * Follows Single Responsibility Principle - focused only on score fusion logic.
 * Implements weighted score combination for hybrid search.
 *
 * Algorithm:
 * 1. Normalize BM25 scores (divide by max score)
 * 2. Convert vector distances to similarity scores (1 / (1 + distance))
 * 3. Apply configured weights to each score type
 * 4. Combine scores for documents found in both searches
 * 5. Sort by final hybrid score
 */
public class HybridScoreFusion {

    private static final Logger logger = LoggerFactory.getLogger(HybridScoreFusion.class);

    private final RetrievalConfig config;

    public HybridScoreFusion(RetrievalConfig config) {
        this.config = config;
    }

    /**
     * Fuse BM25 and vector search results into a single ranked list.
     *
     * @param bm25Results Results from BM25 keyword search
     * @param vectorResults Results from vector semantic search
     * @param topK Number of final results to return
     * @return List of fused results sorted by hybrid score
     */
    public List<Map<String, Object>> fuse(
            List<SearchResult> bm25Results,
            List<VectorSearchResult> vectorResults,
            int topK
    ) {
        logger.debug("Fusing {} BM25 results and {} vector results",
                bm25Results.size(), vectorResults.size());

        Map<String, HybridScore> hybridScores = new HashMap<>();

        // Step 1: Add BM25 scores (normalize by max score)
        addBM25Scores(bm25Results, hybridScores);

        // Step 2: Add Vector scores (convert distance to similarity)
        addVectorScores(vectorResults, hybridScores);

        // Step 3: Sort by hybrid score and return top-K
        List<Map<String, Object>> results = hybridScores.values().stream()
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .map(this::toResultMap)
                .collect(Collectors.toList());

        logger.debug("Hybrid fusion produced {} results", results.size());
        return results;
    }

    /**
     * Add BM25 scores to the hybrid score map.
     */
    private void addBM25Scores(List<SearchResult> bm25Results, Map<String, HybridScore> hybridScores) {
        if (bm25Results.isEmpty()) {
            return;
        }

        // Normalize by max score
        float maxBm25 = bm25Results.stream()
                .map(SearchResult::getScore)
                .max(Float::compare)
                .orElse(1.0f);

        for (SearchResult result : bm25Results) {
            String docId = getDocumentId(result.getContent(), result.getFilename());
            float normalizedScore = maxBm25 > 0 ? result.getScore() / maxBm25 : 0.0f;
            float weightedScore = config.getBm25Weight() * normalizedScore;

            hybridScores.put(docId, new HybridScore(
                    result.getContent(),
                    createMetadata(result),
                    weightedScore
            ));
        }
    }

    /**
     * Add vector scores to the hybrid score map.
     */
    private void addVectorScores(List<VectorSearchResult> vectorResults, Map<String, HybridScore> hybridScores) {
        for (VectorSearchResult result : vectorResults) {
            Document doc = result.getDocument();
            String docId = getDocumentId(doc);

            // Convert distance to similarity: 1 / (1 + distance)
            float similarity = 1.0f / (1.0f + result.getDistance());
            float weightedScore = config.getVectorWeight() * similarity;

            if (hybridScores.containsKey(docId)) {
                // Document found in both searches - add vector score
                hybridScores.get(docId).addScore(weightedScore);
            } else {
                // Document only in vector search
                hybridScores.put(docId, new HybridScore(
                        doc.text(),
                        ChromaVectorSearch.metadataToMap(doc.metadata()),
                        weightedScore
                ));
            }
        }
    }

    /**
     * Convert HybridScore to result map format.
     */
    private Map<String, Object> toResultMap(HybridScore hs) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", hs.getContent());
        result.put("metadata", hs.getMetadata());
        result.put("confidence", (double) hs.getScore());
        return result;
    }

    /**
     * Get a stable document ID for deduplication.
     * Uses global_chunk_id from metadata, falls back to filename + content hash.
     */
    private String getDocumentId(Document doc) {
        Map<String, String> metadata = ChromaVectorSearch.metadataToMap(doc.metadata());
        String chunkId = metadata.get("global_chunk_id");
        if (chunkId != null) {
            return chunkId;
        }

        // Fallback: use filename + content hash
        String filename = metadata.getOrDefault("filename", "unknown");
        int contentHash = doc.text().hashCode();
        return filename + ":" + contentHash;
    }

    /**
     * Get document ID from BM25 result.
     */
    private String getDocumentId(String content, String filename) {
        return filename + ":" + content.hashCode();
    }

    /**
     * Create metadata map from SearchResult.
     */
    private Map<String, String> createMetadata(SearchResult result) {
        Map<String, String> metadata = new HashMap<>();
        if (result.getFilename() != null) {
            metadata.put("filename", result.getFilename());
        }
        if (result.getId() != null) {
            metadata.put("id", result.getId());
        }
        return metadata;
    }

    /**
     * Internal class to hold hybrid scores during fusion.
     */
    private static class HybridScore {
        private final String content;
        private final Map<String, String> metadata;
        private float score;

        HybridScore(String content, Map<String, String> metadata, float score) {
            this.content = content;
            this.metadata = metadata;
            this.score = score;
        }

        void addScore(float additionalScore) {
            this.score += additionalScore;
        }

        String getContent() { return content; }
        Map<String, String> getMetadata() { return metadata; }
        float getScore() { return score; }
    }
}
