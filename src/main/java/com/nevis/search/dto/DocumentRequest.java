package com.nevis.search.dto;

import java.util.UUID;

public record DocumentRequest(UUID clientId, String title, String content) {
}
