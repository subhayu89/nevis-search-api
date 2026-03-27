package com.nevis.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.search.config.TestEmbeddingConfig;
import com.nevis.search.config.TestEmbeddingConfig.StubEmbeddingService;
import com.nevis.search.dto.DocumentRequest;
import com.nevis.search.model.Client;
import com.nevis.search.repository.ClientRepository;
import com.nevis.search.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestEmbeddingConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:controller-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        embeddingService.setVector("hello world", List.of(1.0, 0.0, 0.0));
        embeddingService.setVector("address proof", List.of(1.0, 0.0, 0.0));
        embeddingService.setVector("address proof requirements", List.of(1.0, 0.0, 0.0));

        Client client = new Client();
        client.setName("Acme Bank");
        client.setDescription("Provides address proof letters");
        clientRepository.save(client);
    }

    @Test
    void health_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void createDocumentAndSearch_shouldReturnDocumentResults() throws Exception {
        DocumentRequest request = new DocumentRequest(null, "Test doc", "address proof requirements");

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test doc"))
                .andExpect(jsonPath("$.content").value("address proof requirements"));

        mockMvc.perform(get("/api/search").param("q", "address proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[*].type").isArray());
    }
}
