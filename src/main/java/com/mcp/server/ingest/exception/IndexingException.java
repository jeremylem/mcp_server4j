package com.mcp.server.ingest.exception;

/**
 * Exception thrown when keyword indexing or search fails.
 */
public class IndexingException extends IngestionException {

    public IndexingException(String message) {
        super(message);
    }

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}
