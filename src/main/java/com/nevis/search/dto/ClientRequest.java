package com.nevis.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ClientRequest(
        @JsonProperty("first_name")
        @NotBlank(message = "first_name is required")
        String firstName,
        @JsonProperty("last_name")
        @NotBlank(message = "last_name is required")
        String lastName,
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        String description,
        @JsonProperty("social_links")
        List<String> socialLinks
) {
}
