package com.mcp.server.ingest.api;

import com.mcp.server.ingest.exception.DocumentLoadException;
import dev.langchain4j.data.document.Document;

import java.nio.file.Path;

/**
 * Parses a document from a file path.
 *
 * This is the Strategy pattern for handling different document formats
 * (markdown, PDF, DOCX, etc.) with a common interface.
 */
public interface DocumentParser {

    /**
     * Check if this parser supports the given file.
     *
     * @param path File path to check
     * @return true if this parser can handle the file
     */
    boolean supports(Path path);

    /**
     * Parse the document from the given path.
     *
     * @param path File path to parse
     * @return Parsed document with metadata
     * @throws DocumentLoadException if parsing fails
     */
    Document parse(Path path);
}
