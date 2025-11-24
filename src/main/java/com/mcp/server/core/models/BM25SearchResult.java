package com.mcp.server.core.models;

/**
 * Result object from BM25 keyword search.
 *
 * Represents a single search result from the Lucene BM25 index,
 * containing the document content, metadata, and relevance score.
 */
public class BM25SearchResult {

    private final String id;
    private final String content;
    private final String filename;
    private final float score;

    /**
     * Constructor for BM25SearchResult.
     *
     * @param id Document/chunk ID
     * @param content Text content of the document
     * @param filename Original filename
     * @param score BM25 relevance score (higher is more relevant)
     */
    public BM25SearchResult(String id, String content, String filename, float score) {
        this.id = id;
        this.content = content;
        this.filename = filename;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getFilename() {
        return filename;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "BM25SearchResult{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", score=" + score +
                ", contentPreview='" + getContentPreview() + '\'' +
                '}';
    }

    /**
     * Get a preview of the content (first 100 characters).
     */
    public String getContentPreview() {
        if (content == null) {
            return "";
        }
        return content.length() > 100
                ? content.substring(0, 100) + "..."
                : content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BM25SearchResult that = (BM25SearchResult) o;

        if (Float.compare(that.score, score) != 0) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        return filename != null ? filename.equals(that.filename) : that.filename == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + (score != 0.0f ? Float.floatToIntBits(score) : 0);
        return result;
    }
}
