# MCP Server 4J - Local Knowledge Base

Java implementation of a local knowledge base using the Model Context Protocol (MCP). Query your documents with hybrid search (BM25 + vector similarity).

## Features

- Hybrid search (BM25 keyword + vector semantic similarity)
- SOLID architecture with interface-driven design
- Spring Boot dependency injection
- Persistent BM25 index + ChromaDB vector storage
- MCP protocol support
- Multi-format: PDF, Markdown, TXT via Apache Tika

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for local development)
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

**Ingestion** (`com.mcp.server.ingest`)

- MultiFormatDocumentLoader - Loads PDF, Markdown, TXT via Apache Tika
- RecursiveDocumentChunker - Splits into 512-char chunks, 50 overlap
- ChromaVectorStore - Embeddings via all-MiniLM-L6-v2
- LuceneBM25Indexer - Persistent BM25 keyword index

**Retrieval** (`com.mcp.server.retrieval`)

- BaselineRetriever - Hybrid search orchestration
- ChromaVectorSearch - Semantic search via LangChain4j
- HybridScoreFusion - 30% BM25 + 70% Vector (default)
- QueryController - REST API

**Config** (`com.mcp.server.core.config`)

- IngestConfig - Chunking params
- RetrievalConfig - Fusion weights
- McpServerConfiguration - Spring beans

Core interfaces: QueryService, DocumentManager, DocumentChunker, Initializable

## Differences from Python Version

| Aspect               | Python Version                           | Java Version                              |
| -------------------- | ---------------------------------------- | ----------------------------------------- |
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

Advantages:

- Type safety and compile-time checks
- Spring Boot DI and externalized config
- Lucene native BM25, ONNX embeddings
- Better IDE support for refactoring

Tradeoffs:

- More verbose (~10x code size)
- Slower development
- Higher memory (~500MB vs ~200MB)
- Maven vs pip complexity

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

Test corpus (29 markdown files, 873 chunks):

- Ingestion: ~30 seconds
- Query: ~20-30ms average
- Accuracy: 100% R@5
- Memory: ~500MB Java heap + ChromaDB

## References

- [Model Context Protocol](https://github.com/anthropics/mcp)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [LangChain4j](https://docs.langchain4j.dev)
- [Apache Lucene](https://lucene.apache.org/)
- [ChromaDB](https://docs.trychroma.com/)
