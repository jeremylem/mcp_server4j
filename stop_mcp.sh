#!/bin/bash

echo "Stopping MCP services (Java)..."
docker-compose down

echo ""
echo "MCP services stopped and cleaned up."
echo ""
echo "To remove all data (including ChromaDB volume):"
echo "  docker-compose down -v"
echo ""
