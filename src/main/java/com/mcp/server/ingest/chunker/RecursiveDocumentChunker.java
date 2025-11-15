package com.mcp.server.ingest.chunker;

import com.mcp.server.ingest.api.DocumentChunker;
import com.mcp.server.ingest.exception.DocumentChunkException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunks documents using recursive character-based splitting.
 *
 * Wraps LangChain4j's DocumentSplitters.recursive() to provide
 * configurable chunk size and overlap.
 */
public class RecursiveDocumentChunker implements DocumentChunker {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveDocumentChunker.class);

    private final int chunkSize;
    private final int chunkOverlap;
    private final DocumentSplitter splitter;

    /**
     * Constructor with configurable chunk parameters.
     *
     * @param chunkSize Maximum characters per chunk
     * @param chunkOverlap Number of overlapping characters between chunks
     */
    public RecursiveDocumentChunker(int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunkOverlap cannot be negative");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be less than chunkSize");
        }

        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
    }

    @Override
    public List<Document> chunkDocuments(List<Document> documents) {
        logger.info("-".repeat(60));
        logger.info("Chunking documents ({} chars, {} overlap)...", chunkSize, chunkOverlap);

        try {
            // Split documents into text segments
            List<TextSegment> segments = splitter.splitAll(documents);

            // Convert TextSegments back to Documents for BM25 indexing
            List<Document> chunks = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                Metadata metadata = segment.metadata();

                // Add global chunk ID
                metadata.put("global_chunk_id", i);

                // Create Document from TextSegment
                Document chunkDoc = Document.from(segment.text(), metadata);
                chunks.add(chunkDoc);
            }

            logger.info("Created {} chunks", chunks.size());
            return chunks;

        } catch (Exception e) {
            throw new DocumentChunkException("Failed to chunk documents", e);
        }
    }
}
