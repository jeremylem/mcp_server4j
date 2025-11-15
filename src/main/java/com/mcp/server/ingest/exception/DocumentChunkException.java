package com.mcp.server.ingest.exception;

/**
 * Exception thrown when document chunking fails.
 */
public class DocumentChunkException extends IngestionException {

    public DocumentChunkException(String message) {
        super(message);
    }

    public DocumentChunkException(String message, Throwable cause) {
        super(message, cause);
    }
}
