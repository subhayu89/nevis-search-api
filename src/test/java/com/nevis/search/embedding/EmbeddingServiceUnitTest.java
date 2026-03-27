package com.nevis.search.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    }

    @Test
    void getEmbedding_shouldReturnVector() {
        server.expect(once(), requestTo("http://localhost:11434/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"data":[{"embedding":[1.0,2.0,3.0]}]}
                        """, MediaType.APPLICATION_JSON));

        var result = embeddingService.getEmbedding("hello");

        assertEquals(3, result.size());
        assertEquals(1.0, result.get(0));
    }

    @Test
    void getEmbedding_shouldRejectMissingData() {
        server.expect(once(), requestTo("http://localhost:11434/v1/embeddings"))
                .andRespond(withSuccess("""
                        {"data":[]}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> embeddingService.getEmbedding("hello"));
    }

    @Test
    void getEmbedding_shouldMapTimeoutFailures() {
        server.expect(once(), requestTo("http://localhost:11434/v1/embeddings"))
                .andRespond(withException(new ResourceAccessException("timeout", new SocketTimeoutException("read timed out"))));

        assertThrows(IllegalStateException.class, () -> embeddingService.getEmbedding("hello"));
    }
}
