# MCP Server 4J - Local Knowledge Base

Java implementation of a local knowledge base using the Model Context Protocol (MCP). Query your documents with hybrid
search (BM25 + vector similarity).

## Features

- **Hybrid search**: BM25 keyword + vector semantic similarity (30% + 70% weights)
- **Dual storage**: In-memory Lucene BM25 + ChromaDB vector store
- **MCP protocol**: Model Context Protocol server implementation
- **Multi-format support**: PDF, Markdown, TXT via Apache Tika

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21+ (for local development)
- Maven 3.8+ (for local development)

### 1. Add Documents

```bash
documents/
├── mybook.pdf
├── notes.md
└── article.txt
```

### 2. Start Services

```bash
docker-compose up -d
# ChromaDB: port 8000
# MCP Server: port 8001
```

### 3. Ingest Documents

```bash
docker-compose run --rm -v "$(pwd)/documents:/docs" mcp-server ingest \
  --docs_dir "/docs" --chroma-host chroma --chroma-port 8000
```

### 4. Query Your Knowledge Base

**Via MCP JSON-RPC endpoint:**

```bash
curl -X POST http://localhost:8001/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "query_knowledge_base",
      "arguments": {
        "query": "What is the CAP theorem?",
        "topK": 5,
        "useHybrid": true
      }
    }
  }'
```

**Via REST API:**

```bash
curl -X POST http://localhost:8001/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the CAP theorem?",
    "topK": 5,
    "useHybrid": true
  }'
```

### Ingestion Pipeline

```
Documents → Finder → Loader → Chunker → BM25 Index + Vector Store
```

**Key Components:**

- `RecursiveDocumentFinder` - Recursively find documents in directory
- `MultiFormatDocumentLoader` - PDF, Markdown, TXT via Apache Tika
- `RecursiveDocumentChunker` - 512-char chunks, 50-char overlap
- `LuceneBM25Indexer` - In-memory keyword index (Apache Lucene)
- `ChromaVectorSearch` - Embeddings via all-MiniLM-L6-v2

### Retrieval Pipeline

```
Query → BM25 Search + Vector Search → Score Fusion → Ranked Results
```

**Key Components:**

- `BaselineRetriever` - Orchestrates hybrid search (30% BM25 + 70% vector)
- `LuceneBM25Indexer` - BM25 keyword search with Lucene
- `ChromaVectorSearch` - Semantic similarity via LangChain4j
- `HybridScoreFusion` - Weighted score combination and normalization
- `KnowledgeBaseTool` - MCP protocol interface

### Core Interfaces

- `KeywordIndexer` - BM25 indexing and search operations
- `DocumentLoader` - Multi-format document parsing
- `DocumentChunker` - Text splitting strategies
- `IngestionPipeline` - End-to-end ingestion workflow

## Differences from Python Version

| Aspect               | Python Version                           | Java Version                              |
|----------------------|------------------------------------------|-------------------------------------------|
| **Language**         | Python 3.11                              | Java 21                                   |
| **Framework**        | FastMCP + FastAPI                        | Spring Boot + MCP protocol                |
| **DI Container**     | Manual wiring                            | Spring IoC container                      |
| **BM25 Library**     | rank-bm25 (in-memory)                    | Apache Lucene (in-memory)                 |
| **Vector Store**     | ChromaDB Python client                   | LangChain4j ChromaDB integration          |
| **Embedding Model**  | Sentence Transformers                    | LangChain4j ONNX (all-MiniLM-L6-v2)       |
| **Document Loading** | LangChain Python loaders                 | Apache Tika (universal)                   |
| **Chunking**         | LangChain RecursiveCharacterTextSplitter | LangChain4j DocumentSplitters.recursive() |
| **Configuration**    | Hardcoded constants                      | Externalized config classes               |
| **Persistence**      | In-memory BM25, ChromaDB volume          | In-memory BM25, ChromaDB volume           |
| **Code Size**        | ~200 lines                               | ~2000 lines                               |   

### Why Java?

**Advantages:**

- Strong type safety and compile-time error detection
- Spring Boot ecosystem (DI, config management, testing)
- Native Lucene BM25 implementation (no external BM25 library needed)
- ONNX runtime for embeddings (no Python dependencies)

**Tradeoffs:**

- More verbose (~10x code size vs Python)
- Higher memory footprint (~500MB vs ~200MB)

## Configuration

### Retrieval Settings

Edit `src/main/resources/application.yml`:

```yaml
retrieval:
  bm25-weight: 0.3           # Keyword importance (0-1)
  vector-weight: 0.7         # Semantic importance (0-1)
  candidate-pool-size: 20    # Candidates before fusion
```

Or set environment variables:

```bash
RETRIEVAL_BM25_WEIGHT=0.3
RETRIEVAL_VECTOR_WEIGHT=0.7
RETRIEVAL_CANDIDATE_POOL_SIZE=20
```

### Ingestion Settings

Chunk size and overlap are configured in the ingestion pipeline:

- Default chunk size: 512 characters
- Default overlap: 50 characters

To customize, modify `RecursiveDocumentChunker` initialization in your configuration.

## Development

### Local Build

```bash
# Compile and package
mvn clean package

# Run unit tests only
mvn clean test

# Run with integration tests (requires Docker for ChromaDB)
mvn clean verify
```

### Docker Build

```bash
# Build image
docker-compose build mcp-server

# Rebuild without cache
docker-compose build --no-cache mcp-server
```

### Running Locally (without Docker)

```bash
# Start ChromaDB
docker run -p 8000:8000 chromadb/chroma:0.4.24

# Build the JAR
mvn clean package

# Run ingestion CLI
java -jar target/mcp-server4j-1.0.0-SNAPSHOT.jar ingest \
  --docs_dir ./documents \
  --chroma-host localhost \
  --chroma-port 8000

# Run MCP server
java -jar target/mcp-server4j-1.0.0-SNAPSHOT.jar
```

## Troubleshooting

### No Search Results

The BM25 index is in-memory and must be rebuilt on each server restart:

```bash
# Re-run ingestion to rebuild BM25 index
docker-compose run --rm -v "$(pwd)/documents:/docs" mcp-server ingest \
  --docs_dir "/docs" --chroma-host chroma --chroma-port 8000
```

### ChromaDB Connection Failed

```bash
# Check ChromaDB is running
docker-compose ps chroma

# Check ChromaDB logs
docker-compose logs chroma

# Restart ChromaDB
docker-compose restart chroma
```

### No Results from Vector Search

```bash
# Check ChromaDB has documents
curl http://localhost:8000/api/v1/collections/baseline_kb
```

If count is 0, re-run ingestion.

### Out of Memory

Increase Docker memory limit or Java heap size:

```dockerfile
# In Dockerfile, modify:
ENTRYPOINT ["java", "-Xmx1g", "-jar", "app.jar"]
```

## Performance

Benchmark (29 markdown files, 873 chunks):

- **Ingestion**: ~30 seconds
- **Query latency**: ~20-30ms average
- **Recall@5**: 100% on test queries
- **Memory**: ~500MB Java heap + ChromaDB storage
- **Startup**: ~5 seconds (Spring Boot + model loading)

## References

- [Model Context Protocol](https://github.com/anthropics/mcp)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [LangChain4j](https://docs.langchain4j.dev)
- [Apache Lucene](https://lucene.apache.org/)
- [ChromaDB](https://docs.trychroma.com/)
