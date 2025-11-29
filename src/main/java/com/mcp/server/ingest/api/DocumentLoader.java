package com.mcp.server.ingest.api;

import dev.langchain4j.data.document.Document;

import java.nio.file.Path;
import java.util.List;

/**
 * Loads documents from file paths using appropriate parsers.
 */
public interface DocumentLoader {

    List<Document> loadDocuments(List<Path> paths);
}
