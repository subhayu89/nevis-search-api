package com.nevis.search.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmbeddingServiceUnitTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        embeddingService = new EmbeddingService(restTemplate);
        ReflectionTestUtils.setField(embeddingService, "embeddingBaseUrl", "http://localhost:11434/v1/embeddings");
        ReflectionTestUtils.setField(embeddingService, "embeddingModel", "all-minilm");
        ReflectionTestUtils.setField(embeddingService, "apiKey", "");
        ReflectionTestUtils.setField(embeddingService, "fallbackEnabled", true);
        ReflectionTestUtils.setField(embeddingService, "fallbackDimensions", 16);
    }

    @Test
    void getEmbedding_shouldFallbackWhenProviderReturnsServerError() {
        server.expect(once(), requestTo("http://localhost:11434/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw HttpServerErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error",
                            org.springframework.http.HttpHeaders.of(
                                    java.util.Map.of("Content-Type", java.util.List.of(MediaType.APPLICATION_JSON_VALUE)),
                                    (name, value) -> true
                            ),
                            "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8),
                            StandardCharsets.UTF_8
                    );
                });

        var result = embeddingService.getEmbedding("hello");

        assertEquals(16, result.size());
        assertFalse(result.stream().allMatch(v -> v == 0.0));
    }

    @Test
    void getEmbedding_shouldFallbackWhenProviderTimesOut() {
        server.expect(once(), requestTo("http://localhost:11434/v1/embeddings"))
                .andRespond(withException(new IOException(new SocketTimeoutException("read timed out"))));

        var result = embeddingService.getEmbedding("hello");

        assertEquals(16, result.size());
        assertFalse(result.stream().allMatch(v -> v == 0.0));
    }

    @Test
    void getEmbedding_shouldFallbackWhenProviderReturnsInvalidPayload() {
        server.expect(once(), requestTo("http://localhost:11434/v1/embeddings"))
                .andRespond(withSuccess("""
                        {"data":[]}
                        """, MediaType.APPLICATION_JSON));

        var result = embeddingService.getEmbedding("hello");

        assertEquals(16, result.size());
        assertFalse(result.stream().allMatch(v -> v == 0.0));
    }
}
