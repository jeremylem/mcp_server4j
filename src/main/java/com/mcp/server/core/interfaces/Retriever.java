package com.mcp.server.core.interfaces;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Composite retriever interface combining all retrieval capabilities.
 *
 * This interface is now a facade that extends focused interfaces following
 * the Interface Segregation Principle. New implementations should implement
 * only the specific interfaces they need rather than this composite interface.
 *
 * @deprecated Consider implementing specific interfaces (QueryService, DocumentManager, etc.)
 *             instead of this composite interface for better adherence to ISP.
 */
@Deprecated
public interface Retriever extends QueryService, DocumentManager, Initializable {

    /**
     * Chunk documents using the retriever's chunking strategy.
     *
     * @param documents Raw documents to chunk
     * @return Chunked documents with metadata
     */
    List<Document> chunkDocuments(List<Document> documents);
}
