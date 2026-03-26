package com.nevis.search.utils;

import java.util.List;

public final class SimilarityUtils {

    private SimilarityUtils() {
    }

    public static double cosine(List<Double> a, List<Double> b) {
        double dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
