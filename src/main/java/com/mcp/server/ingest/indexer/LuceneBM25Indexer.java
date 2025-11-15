package com.mcp.server.ingest.indexer;

import com.mcp.server.core.models.SearchResult;
import com.mcp.server.ingest.api.KeywordIndexer;
import com.mcp.server.ingest.exception.IndexingException;
import dev.langchain4j.data.document.Document;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * BM25 keyword indexer using Apache Lucene.
 *
 * Implements KeywordIndexer interface for SOLID compliance.
 * Supports both in-memory (ByteBuffersDirectory) and persistent (FSDirectory) storage.
 *
 * BM25 Parameters:
 * - k1 = 1.2 (term frequency saturation)
 * - b = 0.75 (document length normalization)
 *
 * These match the Python rank-bm25 defaults for consistency.
 */
public class LuceneBM25Indexer implements KeywordIndexer {

    private static final Logger logger = LoggerFactory.getLogger(LuceneBM25Indexer.class);

    // BM25 parameters matching Python's rank-bm25
    private static final float K1 = 1.2f;
    private static final float B = 0.75f;

    private Directory directory;
    private IndexWriter indexWriter;
    private DirectoryReader indexReader;
    private IndexSearcher indexSearcher;
    private final StandardAnalyzer analyzer;
    private final Path indexPath;

    private boolean indexBuilt = false;

    /**
     * Constructor for in-memory indexer (backward compatibility).
     */
    public LuceneBM25Indexer() {
        this(null);
    }

    /**
     * Constructor with optional persistent storage.
     *
     * @param indexPath Path to store index on disk (null for in-memory)
     */
    public LuceneBM25Indexer(Path indexPath) {
        this.analyzer = new StandardAnalyzer();
        this.indexPath = indexPath;

        if (indexPath != null) {
            logger.info("LuceneBM25Indexer configured for persistent storage: {}", indexPath);
        } else {
            logger.info("LuceneBM25Indexer configured for in-memory storage");
        }
    }

    /**
     * Check if a persisted index exists on disk.
     *
     * @return true if index exists on disk, false otherwise
     */
    public boolean indexExistsOnDisk() {
        if (indexPath == null) {
            return false;
        }

        try {
            return Files.exists(indexPath) && DirectoryReader.indexExists(FSDirectory.open(indexPath));
        } catch (IOException e) {
            logger.warn("Error checking if index exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Load an existing index from disk.
     *
     * @throws IndexingException if loading fails or index path is not configured
     */
    public void loadIndex() {
        if (indexPath == null) {
            throw new IndexingException("Cannot load index: no index path configured");
        }

        if (!indexExistsOnDisk()) {
            throw new IndexingException("Cannot load index: no index found at " + indexPath);
        }

        logger.info("Loading BM25 index from disk: {}", indexPath);

        try {
            // Open existing FSDirectory
            this.directory = FSDirectory.open(indexPath);

            // Open existing index reader
            this.indexReader = DirectoryReader.open(directory);
            this.indexSearcher = new IndexSearcher(indexReader);
            this.indexSearcher.setSimilarity(new BM25Similarity(K1, B));

            this.indexBuilt = true;

            int docCount = indexReader.numDocs();
            logger.info("BM25 index loaded successfully with {} documents", docCount);

        } catch (IOException e) {
            throw new IndexingException("Failed to load BM25 index from " + indexPath, e);
        }
    }

    @Override
    public void buildIndex(List<Document> documents) {
        logger.info("Building Lucene BM25 index with {} documents", documents.size());

        try {
            // Create directory (FSDirectory for persistence, ByteBuffersDirectory for in-memory)
            if (indexPath != null) {
                // Ensure parent directory exists
                if (!Files.exists(indexPath.getParent())) {
                    Files.createDirectories(indexPath.getParent());
                }
                this.directory = FSDirectory.open(indexPath);
                logger.info("Using persistent storage: {}", indexPath);
            } else {
                this.directory = new ByteBuffersDirectory();
                logger.info("Using in-memory storage");
            }

            // Configure index writer with BM25 similarity
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setSimilarity(new BM25Similarity(K1, B));

            this.indexWriter = new IndexWriter(directory, config);

            // Index all documents
            int indexed = 0;
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);

                org.apache.lucene.document.Document luceneDoc =
                        new org.apache.lucene.document.Document();

                // Add text content (indexed and stored)
                String content = doc.text();
                if (content != null && !content.isEmpty()) {
                    luceneDoc.add(new TextField("content", content, Field.Store.YES));
                } else {
                    // Skip empty documents
                    continue;
                }

                // Add document ID (stored, not indexed for search)
                String id = doc.metadata().getString("id");
                if (id == null) {
                    id = String.valueOf(i);
                }
                luceneDoc.add(new StringField("id", id, Field.Store.YES));

                // Add filename metadata (stored)
                String filename = doc.metadata().getString("filename");
                if (filename != null) {
                    luceneDoc.add(new StringField("filename", filename, Field.Store.YES));
                }

                indexWriter.addDocument(luceneDoc);
                indexed++;
            }

            // Commit changes
            indexWriter.commit();

            // Create searcher with BM25 similarity
            this.indexReader = DirectoryReader.open(indexWriter);
            this.indexSearcher = new IndexSearcher(indexReader);
            this.indexSearcher.setSimilarity(new BM25Similarity(K1, B));

            this.indexBuilt = true;

            if (indexPath != null) {
                logger.info("BM25 index built and persisted successfully with {} documents at {}", indexed, indexPath);
            } else {
                logger.info("BM25 index built successfully (in-memory) with {} documents", indexed);
            }

        } catch (IOException e) {
            throw new IndexingException("Failed to build BM25 index", e);
        }
    }

    @Override
    public List<SearchResult> search(String queryText, int topK) {
        if (!indexBuilt) {
            throw new IndexingException("Index not built yet. Call buildIndex() first.");
        }

        logger.debug("Searching BM25 index for: '{}' (topK={})", queryText, topK);

        try {
            // Parse query
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(QueryParser.escape(queryText));

            // Execute search
            TopDocs topDocs = indexSearcher.search(query, topK);

            // Convert results to SearchResult objects
            List<SearchResult> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = indexSearcher.doc(scoreDoc.doc);

                String id = doc.get("id");
                String content = doc.get("content");
                String filename = doc.get("filename");
                float score = scoreDoc.score;

                results.add(new SearchResult(id, content, filename, score));
            }

            logger.debug("BM25 search returned {} results", results.size());
            return results;

        } catch (Exception e) {
            throw new IndexingException("Failed to search BM25 index for query: " + queryText, e);
        }
    }

    @Override
    public boolean isIndexBuilt() {
        return indexBuilt;
    }

    @Override
    public void close() {
        if (indexReader != null) {
            try {
                indexReader.close();
            } catch (IOException e) {
                logger.warn("Error closing index reader", e);
            }
        }

        if (indexWriter != null) {
            try {
                indexWriter.close();
            } catch (IOException e) {
                logger.warn("Error closing index writer", e);
            }
        }

        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                logger.warn("Error closing directory", e);
            }
        }

        indexBuilt = false;
        logger.debug("BM25 index closed");
    }
}
