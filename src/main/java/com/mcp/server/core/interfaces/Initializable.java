package com.mcp.server.core.interfaces;

/**
 * Marks a component that requires initialization before use.
 *
 * Follows Interface Segregation Principle - focused only on lifecycle management.
 * Used for components that need to build indices, load data, or perform setup.
 */
public interface Initializable {

    /**
     * Initialize the component.
     *
     * This is typically called once at application startup to build necessary
     * indices or load required data from persistent storage.
     *
     * @throws RuntimeException if initialization fails
     */
    void initialize();
}
