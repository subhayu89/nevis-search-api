package com.nevis.search.dto;

import java.util.UUID;

public record ClientResponse(UUID id, String name, String description) {
}
