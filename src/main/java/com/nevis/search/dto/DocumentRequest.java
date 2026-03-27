package com.nevis.search.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record DocumentRequest(
        UUID clientId,
        @NotBlank(message = "title is required") String title,
        @NotBlank(message = "content is required") String content
) {
}
