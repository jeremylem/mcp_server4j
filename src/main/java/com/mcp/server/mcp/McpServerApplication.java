package com.mcp.server.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot MCP Server Application.
 * <p>
 * Exposes RAG capabilities through Model Context Protocol (MCP) using Spring AI.
 * <p>
 * Architecture:
 * - Spring AI MCP Server handles protocol communication (SSE transport)
 * - BaselineRetriever provides hybrid search (BM25 + Vector)
 * - ChromaDB for vector storage
 * - Lucene BM25 for keyword search
 * <p>
 * Usage:
 * java -jar mcp-server4j.jar
 * <p>
 * The server communicates via SSE (Server-Sent Events) at /sse endpoint following the MCP protocol.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.mcp.server"})
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
