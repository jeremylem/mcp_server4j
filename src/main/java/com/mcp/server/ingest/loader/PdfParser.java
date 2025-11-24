package com.mcp.server.ingest.loader;

import com.mcp.server.ingest.api.DocumentParser;
import com.mcp.server.ingest.exception.DocumentLoadException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.apache.pdfbox.Loader;
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

    public PdfParser() {
        this.parser = new ApacheTikaDocumentParser();
    }

    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().endsWith(".pdf");
    }

    @Override
    public Document parse(Path path) {
        logger.debug("Parsing PDF: {}", path.getFileName());

        Document document = null;

        if (isEncrypted(path)) {
            document = parseWithPdfBox(path);
        } else {
            try {
                document = parseWithTika(path);
            } catch (DocumentLoadException e) {
                logger.warn("Tika failed, trying PDFBox fallback for: {}", path.getFileName());
                document = parseWithPdfBox(path);
            }
        }

        return document;
    }

    /**
     * Check if PDF is encrypted.
     */
    private boolean isEncrypted(Path path) {
        try (PDDocument pdDocument = Loader.loadPDF(path.toFile())) {
            return pdDocument.isEncrypted();
        } catch (Exception e) {
            logger.warn("Could not check encryption status for {}: {}", path.getFileName(), e.getMessage());
            return false;
        }
    }

    /**
     * Parse PDF using Apache Tika.
     */
    private Document parseWithTika(Path path) {
        try {
            Document doc = FileSystemDocumentLoader.loadDocument(path, parser);

            // Add custom metadata
            doc.metadata().put("source", path.toString());
            doc.metadata().put("filename", path.getFileName().toString());
            doc.metadata().put("type", "technical_doc");
            doc.metadata().put("parser", "tika");

            logger.info("Successfully loaded PDF via Tika: {} ({} chars)",
                path.getFileName(), doc.text().length());
            return doc;
        } catch (Exception e) {
            throw new DocumentLoadException("Failed to parse PDF with Tika: " + path, e);
        }
    }

    /**
     * Parse PDF using PDFBox (for encrypted PDFs).
     */
    private Document parseWithPdfBox(Path path) {
        try (PDDocument pdDocument = Loader.loadPDF(path.toFile())) {
            // Check permissions
            if (pdDocument.isEncrypted()) {
                AccessPermission permission = pdDocument.getCurrentAccessPermission();
                if (!permission.canExtractContent()) {
                    throw new DocumentLoadException(
                        "PDF is encrypted and does not allow content extraction: " + path);
                }
            }

            // Extract text
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdDocument);

            if (text == null || text.trim().isEmpty()) {
                throw new DocumentLoadException("PDF contains no extractable text: " + path);
            }

            // Create Document with metadata
            Document doc = Document.from(text);
            doc.metadata().put("source", path.toString());
            doc.metadata().put("filename", path.getFileName().toString());
            doc.metadata().put("type", "technical_doc");
            doc.metadata().put("parser", "pdfbox");
            doc.metadata().put("encrypted", String.valueOf(pdDocument.isEncrypted()));
            doc.metadata().put("pages", String.valueOf(pdDocument.getNumberOfPages()));

            logger.info("Successfully loaded PDF via PDFBox: {} ({} chars, {} pages)",
                path.getFileName(), text.length(), pdDocument.getNumberOfPages());

            return doc;
        } catch (Exception e) {
            throw new DocumentLoadException("Failed to parse PDF with PDFBox: " + path, e);
        }
    }
}
