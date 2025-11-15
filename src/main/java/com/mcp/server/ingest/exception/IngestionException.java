package com.mcp.server.ingest.exception;

/**
 * Base runtime exception for all ingestion-related errors.
 *
 * This is a runtime exception to avoid cluttering interface signatures
 * with checked exceptions and to allow flexible error handling.
 */
public class IngestionException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message Error message
     */
    public IngestionException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message Error message
     * @param cause Underlying cause
     */
    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
