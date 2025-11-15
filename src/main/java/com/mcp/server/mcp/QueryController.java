package com.mcp.server.mcp;

import com.mcp.server.core.interfaces.QueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint for querying the knowledge base.
 *
 * Uses QueryService interface (ISP-compliant) instead of composite Retriever.
 * This controller only needs query capabilities, not document management.
 */
@RestController
@RequestMapping("/api")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        List<Map<String, Object>> results = queryService.query(
                request.query(),
                request.topK() != null ? request.topK() : 5,
                request.useHybrid() != null ? request.useHybrid() : true,
                request.filterType() != null ? request.filterType() : null
        );

        return new QueryResponse(
                request.query(),
                results.size(),
                results
        );
    }

    public record QueryRequest(
            String query,
            Integer topK,
            Boolean useHybrid,
            String filterType
    ) {}

    public record QueryResponse(
            String query,
            int resultCount,
            List<Map<String, Object>> results
    ) {}
}
