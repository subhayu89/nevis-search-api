package com.nevis.search.embedding;

import java.util.List;

public interface EmbeddingClient {
    List<Double> getEmbedding(String text);
}
