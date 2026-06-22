package com._202510007517.platform.agent.questionbank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class QuestionBankIndex {
    private final AtomicReference<List<IndexedQuestion>> indexedQuestions = new AtomicReference<>(List.of());

    public void replaceAll(List<IndexedQuestion> questions) {
        indexedQuestions.set(List.copyOf(questions == null ? List.of() : questions));
    }

    public boolean isEmpty() {
        return indexedQuestions.get().isEmpty();
    }

    public List<QuestionChunk> search(List<Double> queryVector,
                                      String userRole,
                                      String topic,
                                      String difficulty,
                                      String type,
                                      int maxCandidates,
                                      double minScore) {
        String normalizedDifficulty = normalizeDifficulty(difficulty);
        List<ScoredQuestion> matches = new ArrayList<>();
        for (IndexedQuestion indexedQuestion : indexedQuestions.get()) {
            QuestionChunk question = indexedQuestion.question();
            if (!isVisible(question.roleScope(), userRole)) {
                continue;
            }
            if (!matchesDifficulty(question.difficulty(), normalizedDifficulty)) {
                continue;
            }
            if (!matchesTopic(question.topic(), topic)) {
                continue;
            }
            if (!matchesType(question.type(), type)) {
                continue;
            }
            double score = cosine(queryVector, indexedQuestion.embedding());
            if (score >= minScore) {
                matches.add(new ScoredQuestion(question, score));
            }
        }
        return matches.stream()
                .sorted(Comparator.comparingDouble(ScoredQuestion::score).reversed())
                .limit(Math.max(1, maxCandidates))
                .map(ScoredQuestion::question)
                .toList();
    }

    private boolean isVisible(String roleScope, String userRole) {
        String scope = normalizeRole(roleScope);
        String role = normalizeRole(userRole);
        return "all".equals(scope) || scope.equals(role);
    }

    private boolean matchesDifficulty(String questionDifficulty, String requestedDifficulty) {
        if (requestedDifficulty == null) {
            return true;
        }
        return requestedDifficulty.equals(normalizeDifficulty(questionDifficulty));
    }

    private boolean matchesTopic(String questionTopic, String requestedTopic) {
        String normalizedRequestedTopic = normalizeText(requestedTopic);
        if (normalizedRequestedTopic == null) {
            return true;
        }
        String normalizedQuestionTopic = normalizeText(questionTopic);
        if (normalizedQuestionTopic == null) {
            return false;
        }
        return normalizedQuestionTopic.equals(normalizedRequestedTopic)
                || normalizedQuestionTopic.contains(normalizedRequestedTopic)
                || normalizedRequestedTopic.contains(normalizedQuestionTopic);
    }

    private boolean matchesType(String questionType, String requestedType) {
        String normalizedRequestedType = normalizeText(requestedType);
        if (normalizedRequestedType == null) {
            return true;
        }
        return normalizedRequestedType.equals(normalizeText(questionType));
    }

    private String normalizeRole(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return "all";
        }
        return normalized.startsWith("role_") ? normalized.substring("role_".length()) : normalized;
    }

    private String normalizeDifficulty(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replace("难度", "").trim();
        return switch (normalized) {
            case "easy", "简单", "入门", "低" -> "简单";
            case "medium", "中", "中等", "中级" -> "中等";
            case "hard", "困难", "高级", "高" -> "困难";
            default -> normalized;
        };
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
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

    public record IndexedQuestion(QuestionChunk question, List<Double> embedding) {
    }

    private record ScoredQuestion(QuestionChunk question, double score) {
    }
}
