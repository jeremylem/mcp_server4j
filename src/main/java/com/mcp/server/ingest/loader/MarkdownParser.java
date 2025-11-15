package com.mcp.server.ingest.loader;

import com.mcp.server.ingest.api.DocumentParser;
import com.mcp.server.ingest.exception.DocumentLoadException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Parses markdown files using LangChain4j's TextDocumentParser.
 *
 * Adds metadata:
 * - source: Full file path
 * - filename: File name only
 * - type: "personal_note"
 */
public class MarkdownParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownParser.class);
    private final TextDocumentParser parser;

    public MarkdownParser() {
        this.parser = new TextDocumentParser();
    }

    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().endsWith(".md");
    }

    @Override
    public Document parse(Path path) {
        try {
            Document doc = FileSystemDocumentLoader.loadDocument(path, parser);

            // Add custom metadata (matching Python implementation)
            doc.metadata().put("source", path.toString());
            doc.metadata().put("filename", path.getFileName().toString());
            doc.metadata().put("type", "personal_note");

            logger.debug("Loaded markdown: {}", path.getFileName());
            return doc;

        } catch (Exception e) {
            throw new DocumentLoadException("Failed to parse markdown: " + path, e);
        }
    }
}
