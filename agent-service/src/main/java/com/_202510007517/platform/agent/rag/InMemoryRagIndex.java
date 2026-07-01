package com._202510007517.platform.agent.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryRagIndex {
    private final AtomicReference<List<IndexedChunk>> indexedChunks = new AtomicReference<>(List.of());

    public void replaceAll(List<IndexedChunk> chunks) {
        indexedChunks.set(List.copyOf(chunks == null ? List.of() : chunks));
    }

    public boolean isEmpty() {
        return indexedChunks.get().isEmpty();
    }

    public List<RagSearchResult> search(List<Double> queryVector, String userRole, int maxChunks, double minScore) {
        return search(queryVector, null, userRole, maxChunks, minScore);
    }

    public List<RagSearchResult> search(List<Double> queryVector, String queryText, String userRole, int maxChunks, double minScore) {
        List<RagSearchResult> results = new ArrayList<>();
        for (IndexedChunk indexedChunk : indexedChunks.get()) {
            if (!isVisible(indexedChunk.chunk().roleScope(), userRole)) {
                continue;
            }
            double score = score(queryVector, queryText, indexedChunk);
            if (score >= minScore) {
                results.add(new RagSearchResult(indexedChunk.chunk(), score));
            }
        }
        return results.stream()
                .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
                .limit(Math.max(1, maxChunks))
                .toList();
    }

    private double score(List<Double> queryVector, String queryText, IndexedChunk indexedChunk) {
        double vectorScore = cosine(queryVector, indexedChunk.embedding());
        if (vectorScore > 0.0) {
            return vectorScore;
        }
        return lexicalScore(queryText, indexedChunk.chunk());
    }

    private boolean isVisible(String roleScope, String userRole) {
        String scope = normalize(roleScope);
        String role = normalize(userRole);
        return "all".equals(scope) || scope.equals(role);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "all";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("role_") ? normalized.substring("role_".length()) : normalized;
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        int size = Math.min(left.size(), right.size());
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < size; i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double lexicalScore(String queryText, RagChunk chunk) {
        Set<String> terms = queryTerms(queryText);
        if (terms.isEmpty()) {
            return 0.0;
        }
        String haystack = normalizeForSearch(chunk.documentTitle())
                + " " + normalizeForSearch(chunk.sectionTitle())
                + " " + normalizeForSearch(chunk.content());
        long matches = terms.stream().filter(haystack::contains).count();
        return matches == 0 ? 0.0 : (double) matches / terms.size();
    }

    private Set<String> queryTerms(String queryText) {
        String normalized = normalizeForSearch(queryText)
                .replace("什么是", "")
                .replace("解释", "")
                .replace("说明", "")
                .replace("如何理解", "")
                .replace("请问", "")
                .replace("一下", "")
                .replace("？", "")
                .replace("?", "");
        Set<String> terms = new LinkedHashSet<>();
        if (!normalized.isBlank()) {
            terms.add(normalized);
        }
        for (String token : normalized.split("[\\s,，.。;；:：!?！？、/\\\\()（）\\[\\]【】《》\"'“”‘’]+")) {
            if (token.length() >= 2) {
                terms.add(token);
            }
        }
        return terms;
    }

    private String normalizeForSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    public record IndexedChunk(RagChunk chunk, List<Double> embedding) {
    }
}
