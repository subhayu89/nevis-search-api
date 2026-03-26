package com.nevis.search.service;

import com.nevis.search.dto.SearchResultDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final DocumentService documentService;

    public SearchService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public List<SearchResultDTO> search(String query) {
        return documentService.search(query);
    }
}
