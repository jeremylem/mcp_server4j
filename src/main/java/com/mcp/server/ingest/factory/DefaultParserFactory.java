package com.mcp.server.ingest.factory;

import com.mcp.server.ingest.api.DocumentParser;
import com.mcp.server.ingest.loader.MarkdownParser;
import com.mcp.server.ingest.loader.PdfParser;

import java.util.List;

/**
 * Default implementation of ParserFactory.
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
