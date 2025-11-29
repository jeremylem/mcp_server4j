# MCP Server 4J - Local Knowledge Base

Java implementation of a local knowledge base using the Model Context Protocol (MCP). Query your documents with hybrid
search (BM25 + vector similarity).

## Features

- **Hybrid search**: BM25 keyword + vector semantic similarity
- **Interface-driven design**: Clean separation of concerns with QueryService, DocumentManager
- **Spring Boot**: Dependency injection and configuration management
- **Persistent storage**: BM25 index + ChromaDB vector store
- **MCP protocol**: Query via JSON-RPC or REST API
- **Multi-format support**: PDF, Markdown, TXT via Apache Tika + PDFBox

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

## Architecture

### Ingestion Pipeline

```
Documents → MultiFormatLoader → Chunker → BM25 Index + Vector Store
```

**Key Components:**

- `MultiFormatDocumentLoader` - PDF, Markdown, TXT via Apache Tika
- `RecursiveDocumentChunker` - 512-char chunks, 50-char overlap
- `LuceneBM25Indexer` - Persistent keyword index
- `ChromaVectorStore` - Embeddings via all-MiniLM-L6-v2

### Retrieval Pipeline

```
Query → BM25 Search + Vector Search → Score Fusion → Ranked Results
```

**Key Components:**

- `BaselineRetriever` - Orchestrates hybrid search
- `ChromaVectorSearch` - Semantic similarity via LangChain4j
- `HybridScoreFusion` - Weighted combination (default: 30% BM25 + 70% vector)
- `KnowledgeBaseTool` - MCP protocol interface
- `QueryController` - REST API endpoint

### Core Interfaces

- `QueryService` - Search operations
- `DocumentManager` - Document ingestion
- `DocumentChunker` - Text splitting
- `Initializable` - Lifecycle management

## Differences from Python Version

| Aspect               | Python Version                           | Java Version                              |
|----------------------|------------------------------------------|-------------------------------------------|
| **Language**         | Python 3.11                              | Java 21                                   |
| **Framework**        | FastMCP + FastAPI                        | Spring Boot + MCP protocol                |
| **DI Container**     | Manual wiring                            | Spring IoC container                      |
| **Architecture**     | Simple functions                         | SOLID-based classes with interfaces       |
| **BM25 Library**     | rank-bm25 (in-memory)                    | Apache Lucene (persistent)                |
| **Vector Store**     | ChromaDB Python client                   | LangChain4j ChromaDB integration          |
| **Embedding Model**  | Sentence Transformers                    | LangChain4j ONNX (all-MiniLM-L6-v2)       |
| **Document Loading** | LangChain Python loaders                 | Apache Tika (universal)                   |
| **Chunking**         | LangChain RecursiveCharacterTextSplitter | LangChain4j DocumentSplitters.recursive() |
| **Configuration**    | Hardcoded constants                      | Externalized config classes               |
| **Testing**          | pytest                                   | JUnit 5                                   |
| **Persistence**      | In-memory BM25, ChromaDB volume          | Persistent BM25 index + ChromaDB          |
| **Code Size**        | ~200 lines                               | ~2000 lines (enterprise patterns)         |
| **Startup**          | Single script                            | Docker entrypoint with dual modes         |

### Why Java?

**Advantages:**

- Strong type safety and compile-time error detection
- Spring Boot ecosystem (DI, config management, testing)
- Native Lucene BM25 with persistent indexes
- ONNX runtime for embeddings (no Python dependencies)
- Enterprise-grade tooling and IDE support
- Clear interfaces and testability

**Tradeoffs:**

- More verbose (~10x code size vs Python)
- Longer development cycles
- Higher memory footprint (~500MB vs ~200MB)
- Build complexity (Maven vs pip)

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

### BM25 Index Not Loading

```bash
# Check if index exists
ls -la data/lucene_bm25/

# Re-run ingestion to rebuild
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

## Build Notes

### Java Version

This project requires **Java 21** (configured in `pom.xml`). Key dependencies:

- **Lombok**: Using `edge-SNAPSHOT` for Java 21+ compatibility
- **JUnit**: 6.0.1 with proper platform launcher support
- **Spring Boot**: 3.5.8
- **Testcontainers**: 2.0.2

If you encounter compilation issues with newer Java versions, ensure:

1. Maven Compiler Plugin is at least 3.14.0
2. Lombok is using the edge-SNAPSHOT version from the edge releases repository
3. JUnit Platform Launcher is included in test dependencies

## References

- [Model Context Protocol](https://github.com/anthropics/mcp)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [LangChain4j](https://docs.langchain4j.dev)
- [Apache Lucene](https://lucene.apache.org/)
- [ChromaDB](https://docs.trychroma.com/)
