package com.nevis.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.search.dto.ClientResponse;
import com.nevis.search.dto.DocumentResponse;
import com.nevis.search.dto.SearchResultDTO;
import com.nevis.search.embedding.EmbeddingClient;
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
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    public DocumentService(DocumentRepository documentRepository,
                           ClientRepository clientRepository,
                           EmbeddingClient embeddingClient,
                           ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.embeddingClient = embeddingClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Document save(Document doc) {
        List<Double> vector = embeddingClient.getEmbedding(doc.getContent());
        try {
            doc.setEmbedding(objectMapper.writeValueAsString(vector));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize embedding", e);
        }
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<SearchResultDTO> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }

        List<Double> queryVector = embeddingClient.getEmbedding(query);

        List<SearchResultDTO> docResults = documentRepository.findAll().stream()
                .map(doc -> new ScoredResult(doc, score(queryVector, doc)))
                .filter(r -> r.score > 0.75)
                .sorted(Comparator.comparingDouble((ScoredResult r) -> r.score).reversed())
                .map(r -> SearchResultDTO.document(toDocumentResponse(r.data), r.score))
                .toList();

        List<SearchResultDTO> clients = clientRepository.search(query).stream()
                .map(client -> SearchResultDTO.client(toClientResponse(client)))
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

    private DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getClientId(),
                document.getTitle(),
                document.getContent(),
                document.getCreatedAt()
        );
    }

    private ClientResponse toClientResponse(Client client) {
        return new ClientResponse(client.getId(), client.getName(), client.getDescription());
    }

    private record ScoredResult(Document data, double score) {}
}
