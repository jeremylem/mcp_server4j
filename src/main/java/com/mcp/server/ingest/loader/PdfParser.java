package com.mcp.server.ingest.loader;

import com.mcp.server.ingest.api.DocumentParser;
import com.mcp.server.ingest.exception.DocumentLoadException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Parses PDF files using Apache Tika via LangChain4j with PDFBox fallback.
 *
 * Handles:
 * - Standard PDFs via Apache Tika
 * - Encrypted/DRM PDFs with restricted permissions (attempts extraction with PDFBox)
 *
 * Adds metadata:
 * - source: Full file path
 * - filename: File name only
 * - type: "technical_doc"
 */
public class PdfParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(PdfParser.class);
    private final ApacheTikaDocumentParser parser;
    private final boolean usePdfBoxFallback;

    public PdfParser() {
        this(true);
    }

    public PdfParser(boolean usePdfBoxFallback) {
        this.parser = new ApacheTikaDocumentParser();
        this.usePdfBoxFallback = usePdfBoxFallback;
    }

    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().endsWith(".pdf");
    }

    @Override
    public Document parse(Path path) {
        try {
            logger.debug("Attempting to parse PDF with Tika: {}", path.getFileName());
            Document doc = FileSystemDocumentLoader.loadDocument(path, parser);

            // Add custom metadata
            doc.metadata().put("source", path.toString());
            doc.metadata().put("filename", path.getFileName().toString());
            doc.metadata().put("type", "technical_doc");
            doc.metadata().put("parser", "tika");

            logger.debug("Successfully loaded PDF via Tika: {} ({} chars)",
                path.getFileName(), doc.text().length());
            return doc;

        } catch (Exception tikaException) {
            logger.warn("Tika failed for PDF {}: {} - {}",
                path.getFileName(), tikaException.getClass().getSimpleName(), tikaException.getMessage());

            // Try PDFBox fallback if enabled
            if (usePdfBoxFallback) {
                try {
                    logger.debug("Attempting PDFBox fallback for: {}", path.getFileName());
                    return parseWithPdfBox(path);
                } catch (Exception pdfBoxException) {
                    logger.error("PDFBox fallback also failed for {}: {} - {}",
                        path.getFileName(), pdfBoxException.getClass().getSimpleName(), pdfBoxException.getMessage());
                }
            }

            // Both methods failed, log root cause and throw
            Throwable rootCause = tikaException;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            if (rootCause != tikaException) {
                logger.error("Root cause: {} - {}",
                    rootCause.getClass().getSimpleName(), rootCause.getMessage());
            }

            throw new DocumentLoadException("Failed to parse PDF: " + path, tikaException);
        }
    }

    /**
     * Fallback parser using PDFBox directly.
     * Useful for encrypted/DRM PDFs that Tika can't handle.
     */
    private Document parseWithPdfBox(Path path) throws Exception {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            // Check if PDF is encrypted
            if (document.isEncrypted()) {
                logger.debug("PDF is encrypted, checking permissions: {}", path.getFileName());
                AccessPermission permission = document.getCurrentAccessPermission();

                if (!permission.canExtractContent()) {
                    throw new DocumentLoadException(
                        "PDF is encrypted and does not allow content extraction: " + path);
                }
                logger.info("PDF is encrypted but allows extraction: {}", path.getFileName());
            }

            // Extract text
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new DocumentLoadException("PDF contains no extractable text: " + path);
            }

            // Create Document with metadata
            Document doc = new Document(text);
            doc.metadata().put("source", path.toString());
            doc.metadata().put("filename", path.getFileName().toString());
            doc.metadata().put("type", "technical_doc");
            doc.metadata().put("parser", "pdfbox");
            doc.metadata().put("encrypted", String.valueOf(document.isEncrypted()));
            doc.metadata().put("pages", String.valueOf(document.getNumberOfPages()));

            logger.info("Successfully loaded encrypted PDF via PDFBox: {} ({} chars, {} pages)",
                path.getFileName(), text.length(), document.getNumberOfPages());

            return doc;
        }
    }
}
