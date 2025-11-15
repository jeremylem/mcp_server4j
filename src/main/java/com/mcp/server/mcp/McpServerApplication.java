package com.mcp.server.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot MCP Server Application.
 *
 * Exposes RAG capabilities through Model Context Protocol (MCP) using Spring AI.
 *
 * Architecture:
 * - Spring AI MCP Server handles protocol communication (STDIO transport)
 * - BaselineRetriever provides hybrid search (BM25 + Vector)
 * - ChromaDB for vector storage
 * - Lucene BM25 for keyword search
 *
 * Usage:
 *   java -jar mcp-server4j.jar
 *
 * The server will communicate via STDIO (standard input/output) following the MCP protocol.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.mcp.server"})
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
