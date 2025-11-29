package com.mcp.server.ingest.cli;

import com.mcp.server.core.config.IngestConfig;
import com.mcp.server.core.models.IngestionRequest;
import com.mcp.server.core.models.IngestionResult;
import com.mcp.server.ingest.api.IngestionPipeline;
import com.mcp.server.ingest.factory.DefaultIngestionComponentFactory;
import com.mcp.server.ingest.factory.IngestionComponentFactory;
import com.mcp.server.ingest.pipeline.DocumentIngestionPipeline;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Command-line interface for document ingestion.
 * <p>
 * Uses the SOLID-compliant pipeline architecture with dependency injection
 * via factory pattern.
 * <p>
 * Usage:
 * java -jar mcp-server4j.jar --docs_dir ./documents
 * java -jar mcp-server4j.jar --docs_dir ./documents --re-ingest
 */
@Command(
        name = "ingest",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Ingest documents into baseline_kb collection with hybrid BM25 + vector search"
)
public class IngestCLI implements Callable<Integer> {

    @Option(
            names = {"--docs_dir"},
            description = "Directory containing documents to ingest",
            defaultValue = "./documents"
    )
    private String docsDir;

    @Option(
            names = {"--re-ingest"},
            description = "Delete collection and re-ingest all documents from scratch"
    )
    private boolean reIngest;

    @Option(
            names = {"--chroma-host"},
            description = "ChromaDB host",
            defaultValue = "chroma"
    )
    private String chromaHost;

    @Option(
            names = {"--chroma-port"},
            description = "ChromaDB port",
            defaultValue = "8000"
    )
    private int chromaPort;

    @Option(
            names = {"--collection"},
            description = "ChromaDB collection name",
            defaultValue = "baseline_kb"
    )
    private String collectionName;

    @Override
    public Integer call() {
        System.out.println("MCP Server 4J - Document Ingestion");
        System.out.println();

        try {
            IngestionPipeline pipeline = createPipeline();
            IngestionRequest request = createRequest();
            IngestionResult result = pipeline.ingest(request);
            printSuccess(result);
            return 0;
        } catch (Exception e) {
            printFailure(e);
            return 1;
        }
    }

    private IngestionPipeline createPipeline() {
        IngestConfig config = new IngestConfig();
        IngestionComponentFactory factory = new DefaultIngestionComponentFactory();

        return DocumentIngestionPipeline.builder()
                .withDocumentFinder(factory.createDocumentFinder())
                .withDocumentLoader(factory.createDocumentLoader())
                .withDocumentChunker(factory.createDocumentChunker(config))
                .withKeywordIndexer(factory.createKeywordIndexer())
                .withVectorStore(factory.createVectorStore(chromaHost, chromaPort, collectionName, config))
                .build();
    }

    private IngestionRequest createRequest() {
        return IngestionRequest.builder()
                .docsDir(Paths.get(docsDir))
                .collectionName(collectionName)
                .chromaHost(chromaHost)
                .chromaPort(chromaPort)
                .reIngest(reIngest)
                .build();
    }

    private void printSuccess(IngestionResult result) {
        System.out.println();
        System.out.println("SUCCESS");
        System.out.println();
        System.out.println("Documents processed: " + result.documentsProcessed());
        System.out.println("Chunks created: " + result.chunksCreated());
        System.out.println("Collection: " + result.collectionName());
        System.out.println();
    }

    private void printFailure(Exception e) {
        System.err.println();
        System.err.println("FAILURE");
        System.err.println();
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new IngestCLI()).execute(args);
        System.exit(exitCode);
    }
}
