package com.nevis.search.dto;

import jakarta.validation.constraints.NotBlank;

public record EmbeddingRequest(@NotBlank(message = "text is required") String text) {}
