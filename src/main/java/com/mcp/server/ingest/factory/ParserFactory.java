package com.mcp.server.ingest.factory;

import com.mcp.server.ingest.api.DocumentParser;

import java.util.List;

/**
 * Factory for creating document parsers.
 * <p>
 * Follows Dependency Inversion Principle and Open/Closed Principle.
 * Allows adding new parsers without modifying existing code.
 */
public interface ParserFactory {

    /**
     * Create a list of document parsers.
     *
     * @return List of available parsers
     */
    List<DocumentParser> createParsers();
}
