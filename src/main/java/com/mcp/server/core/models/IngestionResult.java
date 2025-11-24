package com.mcp.server.core.models;

import lombok.Getter;

/**
 * Result object returned from document ingestion.
 *
 * Mirrors the Python return dictionary from ingest() method:
 * {
 *     'documents_processed': int,
 *     'chunks_created': int,
 *     'total_in_collection': int,
 *     'collection_name': str
 * }
 */
public class IngestionResult {

    @Getter
    private final int documentsProcessed;
    @Getter
    private final int chunksCreated;
    private final int totalInCollection;
    @Getter
    private final String collectionName;

    /**
     * Constructor for IngestionResult.
     *
     * @param documentsProcessed Number of documents loaded and processed
     * @param chunksCreated Number of chunks created from documents
     * @param totalInCollection Total number of chunks in the collection after ingestion
     * @param collectionName Name of the ChromaDB collection
     */
    public IngestionResult(int documentsProcessed, int chunksCreated,
                          int totalInCollection, String collectionName) {
        this.documentsProcessed = documentsProcessed;
        this.chunksCreated = chunksCreated;
        this.totalInCollection = totalInCollection;
        this.collectionName = collectionName;
    }

    @Override
    public String toString() {
        return "IngestionResult{" +
                "documentsProcessed=" + documentsProcessed +
                ", chunksCreated=" + chunksCreated +
                ", totalInCollection=" + totalInCollection +
                ", collectionName='" + collectionName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IngestionResult that = (IngestionResult) o;

        if (documentsProcessed != that.documentsProcessed) return false;
        if (chunksCreated != that.chunksCreated) return false;
        if (totalInCollection != that.totalInCollection) return false;
        return collectionName != null ? collectionName.equals(that.collectionName) : that.collectionName == null;
    }

    @Override
    public int hashCode() {
        int result = documentsProcessed;
        result = 31 * result + chunksCreated;
        result = 31 * result + totalInCollection;
        result = 31 * result + (collectionName != null ? collectionName.hashCode() : 0);
        return result;
    }
}
