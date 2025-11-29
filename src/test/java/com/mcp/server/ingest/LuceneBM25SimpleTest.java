package com.mcp.server.ingest;

import com.mcp.server.core.models.SearchResult;
import com.mcp.server.ingest.indexer.LuceneBM25Indexer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple standalone test for Lucene BM25 indexer.
 */
@DisplayName("Lucene BM25 Simple Test")
class LuceneBM25SimpleTest {

    private LuceneBM25Indexer indexer;

    @BeforeEach
    void setUp() {
        indexer = new LuceneBM25Indexer();
    }

    @AfterEach
    void tearDown() {
        if (indexer != null) {
            indexer.close();
        }
    }

    @Test
    @DisplayName("should build index and perform search")
    void testBuildAndSearch() {
        // Arrange
        List<Document> documents = List.of(
                createDocument("0", "I like football and soccer.", "sports.md"),
                createDocument("1", "The weather is good today.", "weather.md"),
                createDocument("2", "Python is a great programming language.", "python.md")
        );

        // Act - Build index
        indexer.buildIndex(documents);

        // Act - Search
        List<SearchResult> results = indexer.search("football", 5);

        // Assert - Results found
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().content()).contains("football");
        assertThat(results.getFirst().filename()).isEqualTo("sports.md");
        assertThat(results.getFirst().score()).isGreaterThan(0.0f);
    }

    private Document createDocument(String id, String content, String filename) {
        Metadata metadata = Metadata.from(Map.of(
                "id", id,
                "filename", filename
        ));
        return Document.from(content, metadata);
    }
}
