package com.mcp.server.ingest.api;

import java.nio.file.Path;
import java.util.List;

/**
 * Finds documents in a directory based on specific criteria.
 */
public interface DocumentFinder {

    List<Path> findDocuments(Path directory);
}
