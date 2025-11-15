package com.mcp.server.ingest.api;

import com.mcp.server.ingest.exception.DocumentFinderException;

import java.nio.file.Path;
import java.util.List;

/**
 * Finds documents in a directory based on specific criteria.
 *
 * Implementations should handle file filtering (by extension, name patterns, etc.)
 * and directory traversal.
 */
public interface DocumentFinder {

    /**
     * Find documents in the given directory.
     *
     * @param directory Directory to search
     * @return List of document paths matching the criteria
     * @throws DocumentFinderException if directory doesn't exist or search fails
     */
    List<Path> findDocuments(Path directory);
}
