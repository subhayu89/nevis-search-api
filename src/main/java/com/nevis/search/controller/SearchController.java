package com.nevis.search.controller;

import com.nevis.search.dto.*;
import com.nevis.search.embedding.EmbeddingService;
import com.nevis.search.model.Document;
import com.nevis.search.service.DocumentService;
import com.nevis.search.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final EmbeddingService embeddingService;
    private final DocumentService documentService;
    private final SearchService searchService;

    public SearchController(EmbeddingService embeddingService, DocumentService documentService, SearchService searchService) {
        this.embeddingService = embeddingService;
        this.documentService = documentService;
        this.searchService = searchService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    @GetMapping("/search")
    public List<SearchResultDTO> search(@RequestParam String q) {
        return searchService.search(q);
    }

    @PostMapping("/embedding")
    public EmbeddingResponse createEmbedding(@RequestBody EmbeddingRequest request) {
        return new EmbeddingResponse(embeddingService.getEmbedding(request.text()));
    }

    @PostMapping("/documents")
    public DocumentResponse createDocument(@RequestBody DocumentRequest request) {
        Document doc = new Document();
        doc.setClientId(request.clientId());
        doc.setTitle(request.title());
        doc.setContent(request.content());

        Document saved = documentService.save(doc);
        return new DocumentResponse(saved.getId(), saved.getClientId(), saved.getTitle(), saved.getContent(), saved.getCreatedAt());
    }

    public record HealthResponse(String status) {
    }
}
