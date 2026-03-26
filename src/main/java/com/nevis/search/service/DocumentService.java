package com.nevis.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.search.dto.SearchResultDTO;
import com.nevis.search.embedding.EmbeddingService;
import com.nevis.search.model.Client;
import com.nevis.search.model.Document;
import com.nevis.search.repository.ClientRepository;
import com.nevis.search.repository.DocumentRepository;
import com.nevis.search.utils.SimilarityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public DocumentService(DocumentRepository documentRepository,
                           ClientRepository clientRepository,
                           EmbeddingService embeddingService,
                           ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Document save(Document doc) {
        List<Double> vector = embeddingService.getEmbedding(doc.getContent());
        try {
            doc.setEmbedding(objectMapper.writeValueAsString(vector));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize embedding", e);
        }
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<SearchResultDTO> search(String query) {

        List<Double> queryVector = embeddingService.getEmbedding(query);

        List<SearchResultDTO> docResults = documentRepository.findAll().stream()
                .map(doc -> new ScoredResult(doc, score(queryVector, doc)))
                .filter(r -> r.score > 0.75)
                .sorted(Comparator.comparingDouble((ScoredResult r) -> r.score).reversed())
                .map(r -> new SearchResultDTO("document", r.data))
                .toList();

        List<SearchResultDTO> clients = clientRepository.search(query).stream()
                .map(c -> new SearchResultDTO("client", c))
                .toList();

        return Stream.concat(clients.stream(), docResults.stream()).toList();
    }

    private double score(List<Double> queryVector, Document doc) {
        try {
            List<Double> docVector = objectMapper.readValue(doc.getEmbedding(), new TypeReference<>() {});
            return SimilarityUtils.cosine(queryVector, docVector);
        } catch (Exception e) {
            return 0;
        }
    }

    private record ScoredResult(Object data, double score) {}
}
