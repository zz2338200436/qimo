package com._202510007517.platform.agent.questionbank;

import com._202510007517.platform.agent.config.QuestionBankProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class QuestionRagService {
    private static final String NO_MATCH_MESSAGE = "题库暂无匹配题目，请先维护题库或调整主题/难度";

    private final QuestionBankProperties properties;
    private final EmbeddingClient embeddingClient;
    private final QuestionBankIndex index;

    public QuestionRagService(QuestionBankProperties properties,
                              EmbeddingClient embeddingClient,
                              QuestionBankIndex index) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.index = index;
    }

    public Map<String, Object> generateQuestions(String userRole, String topic, String difficulty, int count) {
        return generateQuestions(userRole, topic, difficulty, count, null);
    }

    public Map<String, Object> generateQuestions(String userRole,
                                                 String topic,
                                                 String difficulty,
                                                 int count,
                                                 String type) {
        int requestedCount = Math.max(1, count);
        String normalizedDifficulty = normalizeDifficulty(difficulty);
        if (!properties.isEnabled()) {
            return result(topic, requestedCount, 0, false, normalizedDifficulty, List.of(), "题库题目生成功能未启用。");
        }

        String queryText = buildQueryText(topic, normalizedDifficulty, type);
        List<Double> queryVector = embeddingClient.embed(queryText);
        List<QuestionChunk> candidates = index.search(
                queryVector,
                userRole,
                topic,
                normalizedDifficulty,
                type,
                properties.getMaxCandidates(),
                properties.getMinScore());
        List<Map<String, Object>> questions = candidates.stream()
                .limit(requestedCount)
                .map(this::toPayload)
                .toList();
        int actualCount = questions.size();
        boolean partial = actualCount > 0 && actualCount < requestedCount;
        String message = null;
        if (questions.isEmpty()) {
            message = NO_MATCH_MESSAGE;
        } else if (partial) {
            message = "题库仅匹配到 " + actualCount + "/" + requestedCount + " 道题，请补充题库或放宽主题/难度条件";
        }
        return result(topic, requestedCount, actualCount, partial, normalizedDifficulty, questions, message);
    }

    private Map<String, Object> result(String topic,
                                       int requestedCount,
                                       int actualCount,
                                       boolean partial,
                                       List<Map<String, Object>> questions,
                                       String message) {
        return result(topic, requestedCount, actualCount, partial, "中等", questions, message);
    }

    private Map<String, Object> result(String topic,
                                       int requestedCount,
                                       int actualCount,
                                       boolean partial,
                                       String difficulty,
                                       List<Map<String, Object>> questions,
                                       String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("topic", topic);
        payload.put("count", requestedCount);
        payload.put("actualCount", actualCount);
        payload.put("partial", partial);
        payload.put("difficulty", difficulty);
        payload.put("questions", questions);
        payload.put("message", message);
        return payload;
    }

    private String buildQueryText(String topic, String difficulty, String type) {
        List<String> parts = new ArrayList<>();
        appendIfPresent(parts, topic);
        appendIfPresent(parts, difficulty);
        appendIfPresent(parts, type);
        return String.join(" ", parts);
    }

    private String normalizeDifficulty(String value) {
        String text = Objects.toString(value, "").trim();
        if (text.isBlank()) {
            return "中等";
        }
        text = text.replace("难度", "").trim();
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "easy", "简单", "入门", "低" -> "简单";
            case "medium", "中", "中等", "中级" -> "中等";
            case "hard", "困难", "高级", "高" -> "困难";
            default -> text;
        };
    }

    private Map<String, Object> toPayload(QuestionChunk question) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", question.questionId());
        payload.put("content", question.content());
        payload.put("difficulty", question.difficulty());
        payload.put("type", displayType(question.type()));
        payload.put("score", null);
        payload.put("options", question.options());
        payload.put("answer", question.answer());
        payload.put("analysis", question.analysis());
        payload.put("knowledgePoints", List.of(question.topic()));
        payload.put("sourcePath", question.sourcePath());
        return payload;
    }

    private String displayType(String type) {
        if (type == null) {
            return "题目";
        }
        return switch (type) {
            case "SINGLE_CHOICE" -> "选择题";
            case "MULTIPLE_CHOICE" -> "多选题";
            case "TRUE_FALSE" -> "判断题";
            case "FILL_BLANK" -> "填空题";
            case "SHORT_ANSWER" -> "简答题";
            case "ESSAY" -> "论述题";
            default -> type;
        };
    }

    private void appendIfPresent(List<String> parts, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }
    }

    @FunctionalInterface
    public interface EmbeddingClient {
        List<Double> embed(String text);
    }
}
