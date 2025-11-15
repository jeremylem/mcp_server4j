package com.mcp.server.ingest;

import com.mcp.server.core.models.BM25SearchResult;
import com.mcp.server.ingest.bm25.LuceneBM25Index;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple standalone test for Lucene BM25 index.
 */
@DisplayName("Lucene BM25 Simple Test")
class LuceneBM25SimpleTest {

    private LuceneBM25Index index;

    @BeforeEach
    void setUp() {
        index = new LuceneBM25Index();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (index != null) {
            index.close();
        }
    }

    @Test
    @DisplayName("should build index and perform search")
    void testBuildAndSearch() throws Exception {
        // Arrange
        List<Document> documents = List.of(
            createDocument("0", "I like football and soccer.", "sports.md"),
            createDocument("1", "The weather is good today.", "weather.md"),
            createDocument("2", "Python is a great programming language.", "python.md")
        );

        // Act - Build index
        index.buildIndex(documents);

        // Assert - Index is built
        assertThat(index.isIndexBuilt()).isTrue();

        // Act - Search
        List<BM25SearchResult> results = index.search("football", 5);

        // Assert - Results found
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getContent()).contains("football");
        assertThat(results.get(0).getFilename()).isEqualTo("sports.md");
        assertThat(results.get(0).getScore()).isGreaterThan(0.0f);
    }

    private Document createDocument(String id, String content, String filename) {
        Metadata metadata = Metadata.from(Map.of(
            "id", id,
            "filename", filename
        ));
        return Document.from(content, metadata);
    }
}
