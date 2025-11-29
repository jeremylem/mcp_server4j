package com.mcp.server.ingest.api;

import dev.langchain4j.data.document.Document;

import java.nio.file.Path;

/**
 * Parses a document from a file path.
 */
public interface DocumentParser {

    boolean supports(Path path);

    Document parse(Path path);
}
