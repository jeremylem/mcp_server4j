#!/bin/bash

# --- Usage Function ---
usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS] [DOCS_DIR]

Start the MCP knowledge base server (Java) with optional document ingestion.

OPTIONS:
    --ingest        Run document ingestion before starting server
    --re-ingest     Force full re-ingestion of all documents (deletes existing data)
    --build         Rebuild the Docker image before starting
    --help, -h      Show this help message

ARGUMENTS:
    DOCS_DIR        Path to documents directory (default: documents)

EXAMPLES:
    # Start server only (no ingestion)
    $(basename "$0")

    # Start server with document ingestion
    $(basename "$0") --ingest

    # Start server with full re-ingestion
    $(basename "$0") --re-ingest

    # Start server with custom documents directory
    $(basename "$0") --ingest ./my_docs

    # Rebuild and start server only
    $(basename "$0") --build

EOF
    exit 0
}

# --- Configuration ---
INGEST_FLAG=""
RE_INGEST_FLAG=""
BUILD_FLAG=""
DOCS_DIR="documents"

# --- Argument Handling ---
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --help|-h) usage ;;
        --ingest) INGEST_FLAG="--ingest"; shift ;;
        --re-ingest) RE_INGEST_FLAG="--re-ingest"; INGEST_FLAG="--ingest"; shift ;;
        --build) BUILD_FLAG="--build"; shift ;;
        *) DOCS_DIR="$1"; shift ;;
    esac
done

# --- Check if Documents Directory Exists ---
if [[ ! -d "$DOCS_DIR" ]]; then
    echo "Warning: Documents directory '$DOCS_DIR' does not exist."
    echo "Creating directory..."
    mkdir -p "$DOCS_DIR"
fi

# --- Start ChromaDB First ---
echo "Starting ChromaDB..."
docker-compose up -d chroma

# Wait for ChromaDB to be ready
echo "Waiting for ChromaDB to be ready..."
RETRY_COUNT=0
MAX_RETRIES=30
until curl -s http://localhost:8000/api/v1/version > /dev/null 2>&1 || [ $RETRY_COUNT -eq $MAX_RETRIES ]; do
    echo -n "."
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT + 1))
done
echo ""

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "Error: ChromaDB failed to start after ${MAX_RETRIES} attempts"
    echo "Checking ChromaDB logs:"
    docker-compose logs chroma
    exit 1
fi

echo "ChromaDB is ready!"

# --- Document Ingestion (only if explicitly requested) ---
if [[ -n "$INGEST_FLAG" ]]; then
    if [[ ! -n "$(ls -A "$DOCS_DIR" 2>/dev/null)" ]]; then
        echo "Warning: No documents found in '$DOCS_DIR', skipping ingestion"
    elif [[ -n "$RE_INGEST_FLAG" ]]; then
        echo "Running full re-ingestion with docs directory: $DOCS_DIR"
        docker-compose run --rm -v "$(pwd)/$DOCS_DIR:/docs" mcp-server \
            ingest \
            $RE_INGEST_FLAG \
            --docs_dir "/docs" \
            --chroma-host chroma \
            --chroma-port 8000

        # Check ingestion exit code
        if [[ $? -ne 0 ]]; then
            echo "Warning: Document ingestion encountered errors"
        fi
    else
        echo "Running document ingestion with docs directory: $DOCS_DIR"
        docker-compose run --rm -v "$(pwd)/$DOCS_DIR:/docs" mcp-server \
            ingest \
            --docs_dir "/docs" \
            --chroma-host chroma \
            --chroma-port 8000

        # Check ingestion exit code
        if [[ $? -ne 0 ]]; then
            echo "Warning: Document ingestion encountered errors"
        fi
    fi
else
    echo "Skipping document ingestion (use --ingest or --re-ingest to run ingestion)"
fi

# --- Start MCP Server ---
echo "Starting MCP Server..."
if [[ -n "$BUILD_FLAG" ]]; then
    docker-compose up --build -d
else
    docker-compose up -d
fi

# --- Show Status ---
echo ""
echo "MCP Server (Java) is starting..."
echo ""
echo "Services:"
echo "  - ChromaDB:   http://localhost:8000"
echo "  - MCP Server: Running (STDIO transport)"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f mcp-server"
echo ""
echo "To stop:"
echo "  ./stop_mcp.sh"
echo ""

