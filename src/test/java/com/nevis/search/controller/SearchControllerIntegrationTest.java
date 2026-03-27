package com.nevis.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.search.config.TestEmbeddingConfig;
import com.nevis.search.config.TestEmbeddingConfig.StubEmbeddingService;
import com.nevis.search.dto.ClientRequest;
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
        embeddingService.reset();
        embeddingService.setVector("address proof", List.of(1.0, 0.0, 0.0));
        embeddingService.setVector("address proof requirements", List.of(1.0, 0.0, 0.0));

        Client client = new Client();
        client.setFirstName("Acme");
        client.setLastName("Bank");
        client.setEmail("john.doe@neviswealth.com");
        client.setDescription("Provides address proof letters");
        clientRepository.save(client);
    }

    @Test
    void createClient_shouldReturnCreatedClient() throws Exception {
        ClientRequest request = new ClientRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "Advisor",
                List.of("https://linkedin.com/in/johndoe")
        );

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.first_name").value("John"))
                .andExpect(jsonPath("$.last_name").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void createClientDocumentAndSearch_shouldReturnDocumentResults() throws Exception {
        Client client = clientRepository.findAll().get(0);
        DocumentRequest request = new DocumentRequest("Test doc", "address proof requirements");

        mockMvc.perform(post("/clients/{id}/documents", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test doc"))
                .andExpect(jsonPath("$.content").value("address proof requirements"))
                .andExpect(jsonPath("$.client_id").value(client.getId().toString()));

        mockMvc.perform(get("/search").param("q", "address proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].client").exists())
                .andExpect(jsonPath("$[1].document.title").value("Test doc"))
                .andExpect(jsonPath("$[1].score").value(1.0));
    }

    @Test
    void search_shouldMatchRelatedDocumentTerms() throws Exception {
        Client client = clientRepository.findAll().get(0);
        DocumentRequest request = new DocumentRequest("Residency", "utility bill");

        mockMvc.perform(post("/clients/{id}/documents", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/search").param("q", "address proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].document.title").value("Residency"));
    }

    @Test
    void createClient_shouldReturnValidationErrorForInvalidFields() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"first_name":"","last_name":"","email":"bad-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.firstName").value("first_name is required"))
                .andExpect(jsonPath("$.validationErrors.lastName").value("last_name is required"))
                .andExpect(jsonPath("$.validationErrors.email").value("email must be valid"));
    }

    @Test
    void createDocument_shouldReturnValidationErrorForBlankFields() throws Exception {
        Client client = clientRepository.findAll().get(0);

        mockMvc.perform(post("/clients/{id}/documents", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").value("title is required"))
                .andExpect(jsonPath("$.validationErrors.content").value("content is required"));
    }

    @Test
    void search_shouldReturnBadRequestForBlankQuery() throws Exception {
        mockMvc.perform(get("/search").param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("query must not be blank"));
    }

    @Test
    void createClientDocument_shouldReturnBadGatewayWhenEmbeddingFails() throws Exception {
        Client client = clientRepository.findAll().get(0);
        embeddingService.setFailure("explode", new IllegalStateException("Embedding provider timeout or connection failure"));

        mockMvc.perform(post("/clients/{id}/documents", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Broken","content":"explode"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Embedding provider timeout or connection failure"));
    }
}
