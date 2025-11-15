package com.mcp.server.ingest.bm25;

import com.mcp.server.core.models.BM25SearchResult;
import dev.langchain4j.data.document.Document;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory BM25 index using Apache Lucene.
 *
 * Implements BM25 keyword search to complement vector-based semantic search.
 * Uses ByteBuffersDirectory for in-memory storage (no disk I/O).
 *
 * BM25 Parameters:
 * - k1 = 1.2 (term frequency saturation)
 * - b = 0.75 (document length normalization)
 *
 * These match the Python rank-bm25 defaults for consistency.
 */
public class LuceneBM25Index implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LuceneBM25Index.class);

    // BM25 parameters matching Python's rank-bm25
    private static final float K1 = 1.2f;
    private static final float B = 0.75f;

    private Directory directory;
    private IndexWriter indexWriter;
    private DirectoryReader indexReader;
    private IndexSearcher indexSearcher;
    private final StandardAnalyzer analyzer;

    private boolean indexBuilt = false;

    /**
     * Constructor initializes the analyzer.
     */
    public LuceneBM25Index() {
        this.analyzer = new StandardAnalyzer();
    }

    /**
     * Build the BM25 index from a list of documents.
     *
     * @param documents List of LangChain4j Document objects to index
     * @throws IOException if indexing fails
     */
    public void buildIndex(List<Document> documents) throws IOException {
        logger.info("Building Lucene BM25 index with {} documents", documents.size());

        // Create in-memory directory
        this.directory = new ByteBuffersDirectory();

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

        logger.info("BM25 index built successfully with {} documents", indexed);
    }

    /**
     * Search the BM25 index for documents matching the query.
     *
     * @param queryText Search query string
     * @param topK Maximum number of results to return
     * @return List of BM25SearchResult objects, sorted by relevance (highest score first)
     * @throws Exception if search fails
     */
    public List<BM25SearchResult> search(String queryText, int topK) throws Exception {
        if (!indexBuilt) {
            throw new IllegalStateException("Index not built yet. Call buildIndex() first.");
        }

        logger.debug("Searching BM25 index for: '{}' (topK={})", queryText, topK);

        // Parse query
        QueryParser parser = new QueryParser("content", analyzer);
        Query query;
        try {
            query = parser.parse(QueryParser.escape(queryText));
        } catch (ParseException e) {
            logger.error("Failed to parse query: {}", queryText, e);
            throw new Exception("Failed to parse query: " + queryText, e);
        }

        // Execute search
        TopDocs topDocs = indexSearcher.search(query, topK);

        // Convert results to BM25SearchResult objects
        List<BM25SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            org.apache.lucene.document.Document doc = indexSearcher.doc(scoreDoc.doc);

            String id = doc.get("id");
            String content = doc.get("content");
            String filename = doc.get("filename");
            float score = scoreDoc.score;

            results.add(new BM25SearchResult(id, content, filename, score));
        }

        logger.debug("BM25 search returned {} results", results.size());

        return results;
    }

    /**
     * Close the index and release resources.
     *
     * @throws IOException if closing fails
     */
    @Override
    public void close() throws IOException {
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

    /**
     * Check if the index has been built.
     */
    public boolean isIndexBuilt() {
        return indexBuilt;
    }
}
