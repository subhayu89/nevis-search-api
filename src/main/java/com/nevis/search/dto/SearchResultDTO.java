package com.nevis.search.dto;

public record SearchResultDTO(
        SearchResultType type,
        ClientResponse client,
        DocumentResponse document,
        Double score
) {

    public static SearchResultDTO client(ClientResponse client) {
        return new SearchResultDTO(SearchResultType.CLIENT, client, null, null);
    }

    public static SearchResultDTO document(DocumentResponse document, double score) {
        return new SearchResultDTO(SearchResultType.DOCUMENT, null, document, score);
    }
}
