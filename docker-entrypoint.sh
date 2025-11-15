#!/bin/bash
set -e

# Entrypoint script for MCP Server container
# Can run either ingestion CLI or MCP server based on command

if [ "$1" = "ingest" ]; then
    # Run ingestion CLI with explicit main class
    shift  # Remove 'ingest' from arguments
    exec java -cp app.jar com.mcp.server.ingest.cli.IngestCLI "$@"
else
    # Run MCP Server (default)
    exec java -jar app.jar "$@"
fi
