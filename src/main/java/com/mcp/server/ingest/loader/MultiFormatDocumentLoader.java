package com.mcp.server.ingest.loader;

import com.mcp.server.ingest.api.DocumentLoader;
import com.mcp.server.ingest.api.DocumentParser;
import com.mcp.server.ingest.exception.DocumentLoadException;
import com.mcp.server.ingest.factory.DefaultParserFactory;
import com.mcp.server.ingest.factory.ParserFactory;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads documents using multiple parsers based on file type.
 *
 * Refactored to follow SOLID principles:
 * - Strategy Pattern: Delegates to DocumentParser implementations
 * - Dependency Inversion: Depends on ParserFactory abstraction
 * - Open/Closed: New parsers can be added via factory without modifying this class
 */
public class MultiFormatDocumentLoader implements DocumentLoader {

    private static final Logger logger = LoggerFactory.getLogger(MultiFormatDocumentLoader.class);
    private final List<DocumentParser> parsers;

    /**
     * Constructor with parser injection.
     *
     * @param parsers List of parsers to use
     */
    public MultiFormatDocumentLoader(List<DocumentParser> parsers) {
        if (parsers == null || parsers.isEmpty()) {
            throw new IllegalArgumentException("At least one parser is required");
        }
        this.parsers = parsers;
    }

    /**
     * Constructor with factory injection (preferred for DIP).
     *
     * @param parserFactory Factory to create parsers
     */
    public MultiFormatDocumentLoader(ParserFactory parserFactory) {
        this(parserFactory.createParsers());
    }

    /**
     * Default constructor with standard parsers.
     * Uses DefaultParserFactory.
     */
    public MultiFormatDocumentLoader() {
        this(new DefaultParserFactory());
    }

    @Override
    public List<Document> loadDocuments(List<Path> paths) {
        logger.info("-".repeat(60));
        logger.info("Loading documents...");

        List<Document> documents = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Path path : paths) {
            try {
                Document doc = loadDocument(path);
                documents.add(doc);
                successCount++;
                logger.info("  Loaded {}", path.getFileName());
            } catch (DocumentLoadException e) {
                failureCount++;
                logger.error("  Error loading {}: {}", path.getFileName(), e.getMessage());
                // Continue with other documents instead of failing completely
            }
        }

        logger.info("Total documents loaded: {} (success: {}, failures: {})",
            documents.size(), successCount, failureCount);

        if (documents.isEmpty() && !paths.isEmpty()) {
            throw new DocumentLoadException("Failed to load any documents from " + paths.size() + " files");
        }

        return documents;
    }

    /**
     * Load a single document using the appropriate parser.
     *
     * @param path File path to load
     * @return Parsed document
     * @throws DocumentLoadException if no parser supports the file or parsing fails
     */
    private Document loadDocument(Path path) {
        for (DocumentParser parser : parsers) {
            if (parser.supports(path)) {
                return parser.parse(path);
            }
        }

        throw new DocumentLoadException("No parser found for file: " + path);
    }
}
