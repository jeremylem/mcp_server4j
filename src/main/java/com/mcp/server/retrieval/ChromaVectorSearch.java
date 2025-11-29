package com.mcp.server.retrieval;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vector search using ChromaDB.
 * <p>
 * Provides semantic search capabilities using embeddings.
 */
public record ChromaVectorSearch(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

    private static final Logger logger = LoggerFactory.getLogger(ChromaVectorSearch.class);

    /**
     * Perform vector similarity search.
     *
     * @param query The search query
     * @param topK  Number of results to return
     * @return List of documents with distance scores
     */
    public List<VectorSearchResult> search(String query, int topK) {
        logger.debug("Performing vector search for: '{}' (topK={})", query, topK);

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder requestBuilder =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(topK);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(requestBuilder.build()).matches();

        List<VectorSearchResult> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();

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

        try {
            EmbeddingSearchRequest request = createGetAllRequest();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
            List<Document> documents = convertMatchesToDocuments(matches);

            logger.info("Fetched {} documents from ChromaDB", documents.size());
            return documents;
        } catch (Exception e) {
            logger.error("Failed to fetch documents from ChromaDB", e);
            throw new RuntimeException("Failed to fetch documents from ChromaDB", e);
        }
    }

    private EmbeddingSearchRequest createGetAllRequest() {
        Embedding dummyEmbedding = embeddingModel.embed("document").content();
        return EmbeddingSearchRequest.builder()
                .queryEmbedding(dummyEmbedding)
                .maxResults(100000)
                .minScore(0.0)
                .build();
    }

    private List<Document> convertMatchesToDocuments(List<EmbeddingMatch<TextSegment>> matches) {
        List<Document> documents = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            documents.add(Document.from(segment.text(), segment.metadata()));
        }
        return documents;
    }

    /**
     * Add documents to the vector store.
     *
     * @param documents Documents to add
     */
    public void addDocuments(List<Document> documents) {
        logger.info("Adding {} documents to vector store...", documents.size());

        try {
            List<TextSegment> segments = convertToSegments(documents);
            List<Embedding> embeddings = generateEmbeddings(documents);
            embeddingStore.addAll(embeddings, segments);

            logger.info("Added {} documents to vector store", documents.size());
        } catch (Exception e) {
            logger.error("Failed to add documents to vector store", e);
            throw new RuntimeException("Failed to add documents to vector store", e);
        }
    }

    private List<TextSegment> convertToSegments(List<Document> documents) {
        List<TextSegment> segments = new ArrayList<>();
        for (Document doc : documents) {
            segments.add(TextSegment.from(doc.text(), doc.metadata()));
        }
        return segments;
    }

    private List<Embedding> generateEmbeddings(List<Document> documents) {
        List<Embedding> embeddings = new ArrayList<>();
        for (Document doc : documents) {
            embeddings.add(embeddingModel.embed(doc.text()).content());
        }
        return embeddings;
    }

    /**
     * Convert LangChain4j Metadata to Map for result processing.
     */
    public static Map<String, String> metadataToMap(Metadata metadata) {
        Map<String, String> map = new HashMap<>();
        metadata.toMap().forEach((key, value) ->
                map.put(key, value != null ? value.toString() : null)
        );
        return map;
    }
}
