package com.nevis.search.config;

import com.nevis.search.embedding.EmbeddingClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestEmbeddingConfig {

    @Bean("embeddingService")
    @Primary
    public StubEmbeddingService embeddingService() {
        return new StubEmbeddingService();
    }

    public static class StubEmbeddingService implements EmbeddingClient {
        private final Map<String, List<Double>> vectors = new HashMap<>();
        private final Map<String, RuntimeException> failures = new HashMap<>();

        public void setVector(String text, List<Double> vector) {
            vectors.put(text, vector);
        }

        public void setFailure(String text, RuntimeException exception) {
            failures.put(text, exception);
        }

        public void reset() {
            vectors.clear();
            failures.clear();
        }

        @Override
        public List<Double> getEmbedding(String text) {
            if (failures.containsKey(text)) {
                throw failures.get(text);
            }
            return vectors.getOrDefault(text, List.of());
        }
    }
}
