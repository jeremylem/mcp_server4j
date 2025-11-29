package com.mcp.server.ingest.exception;

/**
 * Base runtime exception for all ingestion-related errors.
 */
public class IngestionException extends RuntimeException {

    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
