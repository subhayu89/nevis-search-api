package com.nevis.search.service;

import com.nevis.search.model.Client;
import com.nevis.search.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client save(Client client) {
        return clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public Client get(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("client not found: " + id));
    }
}
