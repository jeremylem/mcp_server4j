package com.mcp.server.ingest.factory;

import com.mcp.server.ingest.api.DocumentParser;
import com.mcp.server.ingest.loader.MarkdownParser;
import com.mcp.server.ingest.loader.PdfParser;

import java.util.List;

/**
 * Default implementation of ParserFactory.
 *
 * Creates standard parsers (Markdown, PDF).
 * New parsers can be added here or create a custom factory implementation.
 *
 * Follows Open/Closed Principle - new parsers can be added by:
 * 1. Creating a new parser class implementing DocumentParser
 * 2. Adding it to this factory
 * 3. No changes needed to MultiFormatDocumentLoader
 */
public class DefaultParserFactory implements ParserFactory {

    @Override
    public List<DocumentParser> createParsers() {
        return List.of(
                new MarkdownParser(),
                new PdfParser()
        );
    }
}
