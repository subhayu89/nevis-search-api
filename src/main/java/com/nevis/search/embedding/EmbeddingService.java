package com.nevis.search.embedding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${embedding.base-url:http://host.docker.internal:11434/v1/embeddings}")
    private String embeddingBaseUrl;

    @Value("${embedding.model:nomic-embed-text}")
    private String embeddingModel;

    @Value("${embedding.fallback-enabled:false}")
    private boolean fallbackEnabled;

    @Value("${embedding.fallback-dimensions:64}")
    private int fallbackDimensions;

    @Autowired
    public EmbeddingService(
            @Value("${embedding.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${embedding.read-timeout-ms:15000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public EmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Double> getEmbedding(String text) {
        Map<String, Object> request = Map.of(
                "input", text,
                "model", embeddingModel
        );

        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(apiKey)) {
            headers.setBearerAuth(apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(
                    embeddingBaseUrl,
                    entity,
                    Map.class
            );
        } catch (RestClientResponseException ex) {
            if (fallbackEnabled) {
                return fallbackEmbedding(text);
            }
            throw new IllegalStateException("Embedding provider error: " + ex.getRawStatusCode() + " " + ex.getStatusText(), ex);
        } catch (ResourceAccessException ex) {
            if (fallbackEnabled) {
                return fallbackEmbedding(text);
            }
            throw new IllegalStateException("Embedding provider timeout or connection failure: " + embeddingBaseUrl, ex);
        }

        if (response.getBody() == null || response.getBody().get("data") == null) {
            if (fallbackEnabled) {
                return fallbackEmbedding(text);
            }
            throw new IllegalStateException("Embedding provider returned an invalid response");
        }

        List<?> data = (List<?>) response.getBody().get("data");
        if (data.isEmpty()) {
            if (fallbackEnabled) {
                return fallbackEmbedding(text);
            }
            throw new IllegalStateException("Embedding provider returned no embedding data");
        }
        Map<?, ?> embeddingObj = (Map<?, ?>) data.get(0);

        return (List<Double>) embeddingObj.get("embedding");
    }

    private List<Double> fallbackEmbedding(String text) {
        byte[] digest = sha256(text == null ? "" : text);
        double[] vector = new double[Math.max(8, fallbackDimensions)];

        for (int i = 0; i < digest.length; i++) {
            int bucket = i % vector.length;
            vector[bucket] += digest[i];
        }

        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            norm = 1.0;
        }

        List<Double> result = new ArrayList<>(vector.length);
        for (double value : vector) {
            result.add(value / norm);
        }
        return result;
    }

    private byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable for fallback embeddings", e);
        }
    }
}
