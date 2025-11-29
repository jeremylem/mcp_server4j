package com.mcp.server.ingest.finder;

import com.mcp.server.ingest.api.DocumentFinder;
import com.mcp.server.ingest.exception.DocumentFinderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Finds markdown and PDF files in a directory.
 * <p>
 * Filtering rules:
 * - Includes: *.md and *.pdf files
 * - Excludes: README.md and CLAUDE.md in root directory
 * - Includes: README.md and CLAUDE.md in subdirectories of documents/
 */
public class MarkdownAndPdfFinder implements DocumentFinder {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownAndPdfFinder.class);

    @Override
    public List<Path> findDocuments(Path directory) {
        if (!Files.exists(directory)) {
            throw new DocumentFinderException("Directory not found: " + directory);
        }

        if (!Files.isDirectory(directory)) {
            throw new DocumentFinderException("Path is not a directory: " + directory);
        }

        List<Path> mdFiles = new ArrayList<>();
        List<Path> pdfFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String filename = path.getFileName().toString();

                        // Filter markdown files (exclude README.md and CLAUDE.md in root)
                        if (filename.endsWith(".md")) {
                            if (!filename.equals("README.md") && !filename.equals("CLAUDE.md")) {
                                mdFiles.add(path);
                            } else if (path.toString().contains("documents")) {
                                // Include README/CLAUDE if they're in documents/ subdirectory
                                mdFiles.add(path);
                            }
                        }
                        // Include PDF files
                        else if (filename.endsWith(".pdf")) {
                            pdfFiles.add(path);
                        }
                    });
        } catch (IOException e) {
            throw new DocumentFinderException("Failed to scan directory: " + directory, e);
        }

        logger.info("Found {} markdown files", mdFiles.size());
        logger.info("Found {} PDF files", pdfFiles.size());
        logger.info("Total: {} files to process", mdFiles.size() + pdfFiles.size());

        // Combine both lists
        List<Path> allFiles = new ArrayList<>();
        allFiles.addAll(mdFiles);
        allFiles.addAll(pdfFiles);

        return allFiles;
    }
}
