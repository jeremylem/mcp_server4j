package com.mcp.server.ingest.exception;

/**
 * Exception thrown when vector store operations fail.
 */
public class VectorStoreException extends IngestionException {

    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
