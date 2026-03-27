package com.nevis.search.controller;

import com.nevis.search.dto.*;
import com.nevis.search.model.Client;
import com.nevis.search.model.Document;
import com.nevis.search.service.ClientService;
import com.nevis.search.service.DocumentService;
import com.nevis.search.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class SearchController {

    private final ClientService clientService;
    private final DocumentService documentService;
    private final SearchService searchService;

    public SearchController(ClientService clientService, DocumentService documentService, SearchService searchService) {
        this.clientService = clientService;
        this.documentService = documentService;
        this.searchService = searchService;
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@Valid @RequestBody ClientRequest request) {
        Client client = new Client();
        client.setFirstName(request.firstName());
        client.setLastName(request.lastName());
        client.setEmail(request.email());
        client.setDescription(request.description());
        client.setSocialLinks(request.socialLinks() == null ? List.of() : request.socialLinks());

        Client saved = clientService.save(client);
        return new ClientResponse(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail(),
                saved.getDescription(),
                saved.getSocialLinks()
        );
    }

    @PostMapping("/clients/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentRequest request
    ) {
        clientService.get(id);
        Document saved = documentService.saveForClient(id, request.title(), request.content());
        return new DocumentResponse(saved.getId(), saved.getClientId(), saved.getTitle(), saved.getContent(), saved.getCreatedAt());
    }

    @GetMapping("/search")
    public List<SearchResultDTO> search(@RequestParam String q) {
        return searchService.search(q);
    }
}
