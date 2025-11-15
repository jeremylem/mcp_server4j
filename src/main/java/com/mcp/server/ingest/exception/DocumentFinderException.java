package com.mcp.server.ingest.exception;

/**
 * Exception thrown when document discovery fails.
 */
public class DocumentFinderException extends IngestionException {

    public DocumentFinderException(String message) {
        super(message);
    }

    public DocumentFinderException(String message, Throwable cause) {
        super(message, cause);
    }
}
