# MCP Server 4J - Local Knowledge Base (Java)

A Java implementation of a local knowledge base system using the Model Context Protocol (MCP). This project enables AI assistants to intelligently access and query your documents using hybrid search (BM25 + Vector similarity).

## Key Features

- **Hybrid Search**: Combines BM25 keyword matching with vector semantic similarity
- **SOLID Architecture**: Clean separation of concerns with focused interfaces
- **Spring Boot**: Enterprise-grade dependency injection and configuration
- **Persistent Storage**: BM25 index and ChromaDB vector storage survive restarts
- **MCP Protocol**: Compatible with MCP-enabled AI assistants like Claude Code
- **Multi-format Support**: PDF, Markdown, TXT, and more via Apache Tika

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for local development)
- Maven 3.8+ (for local development)

### 1. Add Your Documents
Place your documents in the `documents/` directory:
```bash
documents/
├── mybook.pdf
├── notes.md
└── article.txt
```

### 2. Start Services
```bash
docker-compose up -d
```

This starts:
- ChromaDB on port 8000 (vector database)
- MCP Server on port 8001 (query API)

### 3. Ingest Documents
```bash
docker-compose run --rm -v "$(pwd)/documents:/docs" mcp-server ingest \
  --docs_dir "/docs" --chroma-host chroma --chroma-port 8000
```

### 4. Query Your Knowledge Base
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

### Core Components

**Ingestion Pipeline** (`com.mcp.server.ingest`)
- **MultiFormatDocumentLoader**: Loads PDF, Markdown, TXT files via Apache Tika
- **RecursiveDocumentChunker**: Splits documents into chunks (512 chars, 50 overlap)
- **ChromaVectorStore**: Stores embeddings using all-MiniLM-L6-v2 model
- **LuceneBM25Indexer**: Builds persistent BM25 keyword index

**Retrieval System** (`com.mcp.server.retrieval`)
- **BaselineRetriever**: Orchestrates hybrid search
- **ChromaVectorSearch**: Semantic search via LangChain4j
- **HybridScoreFusion**: Combines BM25 + Vector scores (30% + 70% default)
- **QueryController**: REST API for queries

**Configuration** (`com.mcp.server.core.config`)
- **IngestConfig**: Chunking parameters, document paths
- **RetrievalConfig**: Fusion weights, candidate pool size
- **McpServerConfiguration**: Spring beans and dependency injection

### SOLID Principles

The codebase follows Interface Segregation Principle with focused interfaces:
- **QueryService**: Query operations only
- **DocumentManager**: Document addition/management
- **DocumentChunker**: Document chunking
- **Initializable**: Initialization logic

Components depend only on the interfaces they need, not on composite interfaces.

## Differences from Python Version

| Aspect | Python Version | Java Version |
|--------|---------------|--------------|
| **Language** | Python 3.11 | Java 17 |
| **Framework** | FastMCP + FastAPI | Spring Boot + MCP protocol |
| **DI Container** | Manual wiring | Spring IoC container |
| **Architecture** | Simple functions | SOLID-based classes with interfaces |
| **BM25 Library** | rank-bm25 (in-memory) | Apache Lucene (persistent) |
| **Vector Store** | ChromaDB Python client | LangChain4j ChromaDB integration |
| **Embedding Model** | Sentence Transformers | LangChain4j ONNX (all-MiniLM-L6-v2) |
| **Document Loading** | LangChain Python loaders | Apache Tika (universal) |
| **Chunking** | LangChain RecursiveCharacterTextSplitter | LangChain4j DocumentSplitters.recursive() |
| **Configuration** | Hardcoded constants | Externalized config classes |
| **Testing** | pytest | JUnit 5 |
| **Persistence** | In-memory BM25, ChromaDB volume | Persistent BM25 index + ChromaDB |
| **Code Size** | ~200 lines | ~2000 lines (enterprise patterns) |
| **Startup** | Single script | Docker entrypoint with dual modes |

### Why Java?

**Advantages of Java Implementation:**
1. **Enterprise Ready**: Spring Boot ecosystem, dependency injection, externalized config
2. **Type Safety**: Compile-time checks prevent runtime errors
3. **Performance**: Native BM25 implementation via Lucene, ONNX-based embeddings
4. **Persistence**: BM25 index survives restarts (Python version rebuilds on startup)
5. **Scalability**: Thread-safe components, connection pooling, production patterns
6. **Maintainability**: SOLID architecture, clear interfaces, separation of concerns
7. **IDE Support**: Better refactoring, autocomplete, debugging
8. **Universal Document Support**: Apache Tika handles 1000+ file formats

**Tradeoffs:**
- More verbose code (~10x larger codebase)
- Longer development time for new features
- Higher memory footprint (~500MB vs ~200MB)
- More complex build process (Maven vs pip)

### Key Architectural Improvements

**1. Persistent BM25 Index**
- Python: Rebuilt on every server start from ChromaDB
- Java: Saved to disk at `/data/lucene_bm25`, loaded instantly on startup

**2. Dependency Injection**
- Python: Manual object creation in scripts
- Java: Spring manages lifecycle and dependencies

**3. Configuration Management**
- Python: Hardcoded constants in `retrieval.py`
- Java: `IngestConfig` and `RetrievalConfig` classes with validation

**4. Error Handling**
- Python: Basic try/catch blocks
- Java: Custom exception hierarchy (`VectorStoreException`, `IndexingException`, etc.)

**5. Extensibility**
- Python: Add functions, modify classes directly
- Java: Implement interfaces, extend abstract classes, use factories

## Configuration

### Retrieval Settings

Edit `src/main/java/com/mcp/server/core/config/RetrievalConfig.java`:

```java
@Bean
public RetrievalConfig retrievalConfig() {
    return RetrievalConfig.builder()
        .bm25Weight(0.3)           // Keyword importance (0-1)
        .vectorWeight(0.7)         // Semantic importance (0-1)
        .candidatePoolSize(20)     // Candidates before fusion
        .build();
}
```

### Ingestion Settings

Edit `src/main/java/com/mcp/server/core/config/IngestConfig.java`:

```java
@Bean
public IngestConfig ingestConfig() {
    return IngestConfig.builder()
        .chunkSize(512)            // Characters per chunk
        .chunkOverlap(50)          // Overlap between chunks
        .inputDirectory("./documents")
        .chromaHost("localhost")
        .chromaPort(8000)
        .build();
}
```

## Development

### Local Build

```bash
# Compile and package
mvn clean package

# Run tests
mvn test

# Skip tests
mvn clean package -DskipTests
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
docker run -p 8000:8000 chromadb/chroma

# Run ingestion CLI
java -jar target/mcp-server-1.0.0.jar ingest \
  --docs_dir ./documents \
  --chroma-host localhost \
  --chroma-port 8000

# Run MCP server
java -jar target/mcp-server-1.0.0.jar
```

## Project Structure

```
mcp_server4j/
├── src/
│   ├── main/java/com/mcp/server/
│   │   ├── core/              # Interfaces and configuration
│   │   │   ├── config/        # IngestConfig, RetrievalConfig
│   │   │   ├── interfaces/    # QueryService, DocumentManager, etc.
│   │   │   └── models/        # SearchResult, VectorSearchResult
│   │   ├── ingest/            # Document ingestion pipeline
│   │   │   ├── api/           # VectorStore, KeywordIndexer interfaces
│   │   │   ├── chunker/       # RecursiveDocumentChunker
│   │   │   ├── cli/           # IngestCLI entry point
│   │   │   ├── indexer/       # LuceneBM25Indexer
│   │   │   ├── loader/        # MultiFormatDocumentLoader
│   │   │   ├── pipeline/      # DocumentIngestionPipeline
│   │   │   └── store/         # ChromaVectorStore
│   │   ├── mcp/               # MCP server and REST API
│   │   │   ├── McpServerConfiguration.java
│   │   │   ├── QueryController.java
│   │   │   └── McpServerApplication.java
│   │   └── retrieval/         # Hybrid search implementation
│   │       ├── BaselineRetriever.java
│   │       ├── ChromaVectorSearch.java
│   │       └── HybridScoreFusion.java
│   └── test/java/             # JUnit tests
├── data/                      # Persistent BM25 index
├── chroma_db/                 # ChromaDB volume
├── documents/                 # Your documents
├── pom.xml                    # Maven dependencies
├── Dockerfile                 # Multi-stage build
└── docker-compose.yml         # Services orchestration
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

Typical performance on test corpus (29 markdown files, 873 chunks):

- **Ingestion**: ~30 seconds for 873 chunks
- **Query Latency**: ~20-30ms average
- **Accuracy**: 100% R@5 on test queries
- **Memory**: ~500MB Java heap + ChromaDB

## References

- [Model Context Protocol](https://github.com/anthropics/mcp)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [LangChain4j Documentation](https://docs.langchain4j.dev)
- [Apache Lucene](https://lucene.apache.org/)
- [ChromaDB](https://docs.trychroma.com/)

## License

MIT License - see original Python version for details.
