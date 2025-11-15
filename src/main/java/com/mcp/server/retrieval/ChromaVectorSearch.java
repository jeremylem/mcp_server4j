package com.mcp.server.retrieval;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vector search using ChromaDB.
 *
 * Provides semantic search capabilities using embeddings.
 */
public class ChromaVectorSearch {

    private static final Logger logger = LoggerFactory.getLogger(ChromaVectorSearch.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public ChromaVectorSearch(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Perform vector similarity search.
     *
     * @param query The search query
     * @param topK Number of results to return
     * @param filterMetadata Optional metadata filter
     * @return List of documents with distance scores
     */
    public List<VectorSearchResult> search(String query, int topK, Map<String, String> filterMetadata) {
        logger.debug("Performing vector search for: '{}' (topK={})", query, topK);

        // Embed the query
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // Build search request
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder requestBuilder =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(topK);

        // Add metadata filter if provided
        // Note: LangChain4j 0.36.2 uses simple metadata filtering
        // For now, we'll skip complex filtering and handle it post-search
        // In newer versions, use Filter API for better control

        // Execute search
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(requestBuilder.build()).matches();

        // Convert to VectorSearchResult
        List<VectorSearchResult> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();

            // Convert TextSegment to Document
            Document doc = Document.from(segment.text(), segment.metadata());

            // Distance score (lower is better)
            float distance = (float) (1.0 - match.score()); // Convert similarity to distance

            results.add(new VectorSearchResult(doc, distance));
        }

        logger.debug("Vector search returned {} results", results.size());
        return results;
    }

    /**
     * Get all documents from the store.
     *
     * @return List of all documents
     */
    public List<Document> getAllDocuments() {
        logger.info("Fetching all documents from ChromaDB...");

        // Note: This is a workaround since LangChain4j doesn't provide a direct "getAll" method
        // We search with a very generic query and high topK to get all documents
        // In production, consider using ChromaDB client directly for better control

        try {
            // Dummy embedding for retrieval (use a generic query instead of empty string)
            Embedding dummyEmbedding = embeddingModel.embed("document").content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(dummyEmbedding)
                    .maxResults(100000) // Very large number to get all documents (increased from 10000)
                    .minScore(0.0) // Accept all scores
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

            List<Document> documents = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                Document doc = Document.from(segment.text(), segment.metadata());
                documents.add(doc);
            }

            logger.info("Fetched {} documents from ChromaDB", documents.size());
            return documents;

        } catch (Exception e) {
            logger.error("Failed to fetch documents from ChromaDB", e);
            throw new RuntimeException("Failed to fetch documents from ChromaDB", e);
        }
    }

    /**
     * Add documents to the vector store.
     *
     * @param documents Documents to add
     */
    public void addDocuments(List<Document> documents) {
        logger.info("Adding {} documents to vector store...", documents.size());

        try {
            List<TextSegment> segments = new ArrayList<>();
            List<Embedding> embeddings = new ArrayList<>();

            for (Document doc : documents) {
                // Convert Document to TextSegment
                TextSegment segment = TextSegment.from(doc.text(), doc.metadata());
                segments.add(segment);

                // Generate embedding
                Embedding embedding = embeddingModel.embed(doc.text()).content();
                embeddings.add(embedding);
            }

            // Add to store
            embeddingStore.addAll(embeddings, segments);

            logger.info("Added {} documents to vector store", documents.size());

        } catch (Exception e) {
            logger.error("Failed to add documents to vector store", e);
            throw new RuntimeException("Failed to add documents to vector store", e);
        }
    }

    /**
     * Convert LangChain4j Metadata to Map for result processing.
     */
    public static Map<String, String> metadataToMap(Metadata metadata) {
        Map<String, String> map = new HashMap<>();
        if (metadata != null) {
            metadata.toMap().forEach((key, value) ->
                map.put(key, value != null ? value.toString() : null)
            );
        }
        return map;
    }
}
