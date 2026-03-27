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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class DocumentService {

    private static final double DOCUMENT_SCORE_THRESHOLD = 0.55;
    private static final List<Set<String>> DOCUMENT_CONCEPTS = List.of(
            Set.of("address proof", "proof of address", "utility bill", "bank statement", "residence proof"),
            Set.of("passport", "identity document", "government id", "photo id")
    );

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
                .map(doc -> new ScoredResult(doc, score(query, queryVector, doc)))
                .filter(r -> r.score >= DOCUMENT_SCORE_THRESHOLD)
                .sorted(Comparator.comparingDouble((ScoredResult r) -> r.score).reversed())
                .map(r -> SearchResultDTO.document(toDocumentResponse(r.data), r.score))
                .toList();

        List<SearchResultDTO> clients = clientRepository.search(query).stream()
                .map(client -> SearchResultDTO.client(toClientResponse(client)))
                .toList();

        return Stream.concat(clients.stream(), docResults.stream()).toList();
    }

    private double score(String query, List<Double> queryVector, Document doc) {
        String searchableText = (doc.getTitle() == null ? "" : doc.getTitle() + " ") + (doc.getContent() == null ? "" : doc.getContent());
        return Math.max(vectorScore(queryVector, doc), lexicalScore(query, searchableText));
    }

    private double vectorScore(List<Double> queryVector, Document doc) {
        try {
            List<Double> docVector = objectMapper.readValue(doc.getEmbedding(), new TypeReference<>() {});
            return SimilarityUtils.cosine(queryVector, docVector);
        } catch (Exception e) {
            return 0;
        }
    }

    private double lexicalScore(String query, String documentText) {
        String normalizedDocument = normalize(documentText);
        String normalizedQuery = normalize(query);

        if (normalizedDocument.isBlank() || normalizedQuery.isBlank()) {
            return 0;
        }

        if (normalizedDocument.contains(normalizedQuery)) {
            return 1.0;
        }

        if (sharesConcept(normalizedQuery, normalizedDocument)) {
            return 0.9;
        }

        long overlap = tokenize(normalizedQuery).stream()
                .filter(tokenize(normalizedDocument)::contains)
                .count();
        long queryTerms = tokenize(normalizedQuery).size();
        if (queryTerms == 0) {
            return 0;
        }
        return (double) overlap / queryTerms;
    }

    private boolean sharesConcept(String query, String document) {
        return DOCUMENT_CONCEPTS.stream().anyMatch(concept ->
                concept.stream().anyMatch(query::contains) && concept.stream().anyMatch(document::contains)
        );
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
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
