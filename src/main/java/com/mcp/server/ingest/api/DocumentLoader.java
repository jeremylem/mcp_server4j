package com.mcp.server.ingest.api;

import com.mcp.server.ingest.exception.DocumentLoadException;
import dev.langchain4j.data.document.Document;

import java.nio.file.Path;
import java.util.List;

/**
 * Loads documents from file paths using appropriate parsers.
 *
 * Implementations should delegate to DocumentParser strategies
 * to handle different file formats.
 */
public interface DocumentLoader {

    /**
     * Load documents from the given paths.
     *
     * @param paths List of file paths to load
     * @return List of loaded documents with metadata
     * @throws DocumentLoadException if loading fails
     */
    List<Document> loadDocuments(List<Path> paths);
}
