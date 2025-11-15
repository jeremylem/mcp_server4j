package com.mcp.server.ingest.exception;

/**
 * Exception thrown when document loading or parsing fails.
 */
public class DocumentLoadException extends IngestionException {

    public DocumentLoadException(String message) {
        super(message);
    }

    public DocumentLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
