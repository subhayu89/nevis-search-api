package com.nevis.search.service;

import com.nevis.search.config.TestEmbeddingConfig;
import com.nevis.search.config.TestEmbeddingConfig.StubEmbeddingService;
import com.nevis.search.dto.SearchResultDTO;
import com.nevis.search.model.Client;
import com.nevis.search.model.Document;
import com.nevis.search.repository.ClientRepository;
import com.nevis.search.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestEmbeddingConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:nevis;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SearchServiceIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private StubEmbeddingService embeddingService;

    @BeforeEach
    void setup() {
        documentRepository.deleteAll();
        clientRepository.deleteAll();

        embeddingService.setVector("address proof", List.of(1.0, 0.0, 0.0));
        embeddingService.setVector("sample doc", List.of(1.0, 0.0, 0.0));
        embeddingService.setVector("other", List.of(0.0, 1.0, 0.0));

        Client client = new Client();
        client.setName("Acme Bank");
        client.setDescription("Provides address proof letters");
        clientRepository.save(client);

        Document doc = new Document();
        doc.setClientId(UUID.randomUUID());
        doc.setTitle("Sample doc");
        doc.setContent("sample doc");
        documentService.save(doc);
    }

    @Test
    void semanticSearch_shouldReturnRelevantDocsAndClients() {
        List<SearchResultDTO> results = searchService.search("address proof");

        assertFalse(results.isEmpty());
    }
}
