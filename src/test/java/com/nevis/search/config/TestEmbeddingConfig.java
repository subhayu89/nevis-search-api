package com.nevis.search.config;

import com.nevis.search.embedding.EmbeddingService;
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

    public static class StubEmbeddingService extends EmbeddingService {
        private final Map<String, List<Double>> vectors = new HashMap<>();

        public void setVector(String text, List<Double> vector) {
            vectors.put(text, vector);
        }

        @Override
        public List<Double> getEmbedding(String text) {
            return vectors.getOrDefault(text, List.of());
        }
    }
}
