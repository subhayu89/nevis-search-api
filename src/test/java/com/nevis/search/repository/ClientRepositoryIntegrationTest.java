package com.nevis.search.repository;

import com.nevis.search.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ClientRepositoryIntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @BeforeEach
    void setup() {
        clientRepository.deleteAll();

        Client john = new Client();
        john.setFirstName("John");
        john.setLastName("Doe");
        john.setEmail("john.doe@neviswealth.com");
        john.setDescription("Senior advisor");
        john.setSocialLinks(List.of("https://linkedin.com/in/johndoe"));
        clientRepository.save(john);

        Client jane = new Client();
        jane.setFirstName("Jane");
        jane.setLastName("Smith");
        jane.setEmail("jane@example.com");
        jane.setDescription("Handles onboarding");
        clientRepository.save(jane);
    }

    @Test
    void search_shouldMatchEmail() {
        List<Client> results = clientRepository.search("neviswealth");
        assertEquals(1, results.size());
        assertEquals("John", results.get(0).getFirstName());
    }

    @Test
    void search_shouldMatchFullName() {
        List<Client> results = clientRepository.search("john doe");
        assertEquals(1, results.size());
        assertEquals("john.doe@neviswealth.com", results.get(0).getEmail());
    }

    @Test
    void search_shouldMatchDescription() {
        List<Client> results = clientRepository.search("onboarding");
        assertEquals(1, results.size());
        assertEquals("Jane", results.get(0).getFirstName());
    }
}
