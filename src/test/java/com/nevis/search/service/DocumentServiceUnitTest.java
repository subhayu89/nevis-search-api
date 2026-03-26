package com.nevis.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.search.dto.SearchResultDTO;
import com.nevis.search.model.Document;
import com.nevis.search.repository.ClientRepository;
import com.nevis.search.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceUnitTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientRepository clientRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DocumentService documentService;

    private StubEmbeddingService embeddingService;

    private Document doc;

    @BeforeEach
    void setup() {
        embeddingService = new StubEmbeddingService();
        documentService = new DocumentService(documentRepository, clientRepository, embeddingService, objectMapper);

        doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setClientId(UUID.randomUUID());
        doc.setTitle("Sample");
        doc.setContent("sample content");
    }

    @Test
    void save_shouldEmbedAndPersist() {
        embeddingService.setVector("sample content", List.of(1.0, 0.0));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document saved = documentService.save(doc);

        verify(documentRepository).save(any(Document.class));
        assertEquals(doc.getTitle(), saved.getTitle());
    }

    @Test
    void search_shouldScoreAndFilter() throws Exception {
        embeddingService.setVector("query", List.of(1.0, 0.0));

        Document other = new Document();
        other.setId(UUID.randomUUID());
        other.setContent("other");
        other.setEmbedding(objectMapper.writeValueAsString(List.of(1.0, 0.0)));

        when(documentRepository.findAll()).thenReturn(List.of(other));
        when(clientRepository.search("query")).thenReturn(List.of());

        List<SearchResultDTO> results = documentService.search("query");

        assertEquals(1, results.size());
        verify(documentRepository).findAll();
    }

    private static class StubEmbeddingService extends com.nevis.search.embedding.EmbeddingService {
        private final java.util.Map<String, List<Double>> vectors = new java.util.HashMap<>();

        void setVector(String text, List<Double> vector) {
            vectors.put(text, vector);
        }

        @Override
        public List<Double> getEmbedding(String text) {
            return vectors.getOrDefault(text, List.of());
        }
    }
}
