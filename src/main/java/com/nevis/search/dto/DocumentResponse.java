package com.nevis.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        @JsonProperty("client_id") UUID clientId,
        String title,
        String content,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
}
