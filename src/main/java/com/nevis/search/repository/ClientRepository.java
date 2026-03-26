package com.nevis.search.repository;

import com.nevis.search.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    @Query("select c from Client c where lower(c.name) like lower(concat('%', :q, '%')) or lower(c.description) like lower(concat('%', :q, '%'))")
    List<Client> search(@Param("q") String query);
}
