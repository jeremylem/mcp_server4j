package com.mcp.server.core.models;

import java.nio.file.Path;

/**
 * Request parameters for document ingestion.
 *
 * Uses the Builder pattern for clean, flexible construction.
 */
public class IngestionRequest {

    private final Path docsDir;
    private final String collectionName;
    private final String chromaHost;
    private final int chromaPort;
    private final boolean reIngest;

    private IngestionRequest(Builder builder) {
        this.docsDir = builder.docsDir;
        this.collectionName = builder.collectionName;
        this.chromaHost = builder.chromaHost;
        this.chromaPort = builder.chromaPort;
        this.reIngest = builder.reIngest;
    }

    public Path getDocsDir() {
        return docsDir;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getChromaHost() {
        return chromaHost;
    }

    public int getChromaPort() {
        return chromaPort;
    }

    public boolean isReIngest() {
        return reIngest;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path docsDir;
        private String collectionName = "baseline_kb";
        private String chromaHost = "chroma";
        private int chromaPort = 8000;
        private boolean reIngest = false;

        private Builder() {
        }

        public Builder docsDir(Path docsDir) {
            this.docsDir = docsDir;
            return this;
        }

        public Builder collectionName(String collectionName) {
            this.collectionName = collectionName;
            return this;
        }

        public Builder chromaHost(String chromaHost) {
            this.chromaHost = chromaHost;
            return this;
        }

        public Builder chromaPort(int chromaPort) {
            this.chromaPort = chromaPort;
            return this;
        }

        public Builder reIngest(boolean reIngest) {
            this.reIngest = reIngest;
            return this;
        }

        public IngestionRequest build() {
            if (docsDir == null) {
                throw new IllegalStateException("docsDir is required");
            }
            return new IngestionRequest(this);
        }
    }

    @Override
    public String toString() {
        return String.format("IngestionRequest{docsDir=%s, collection='%s', chromaHost='%s', chromaPort=%d, reIngest=%s}",
            docsDir, collectionName, chromaHost, chromaPort, reIngest);
    }
}
