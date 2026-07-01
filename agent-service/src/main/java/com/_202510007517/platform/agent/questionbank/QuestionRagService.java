package com._202510007517.platform.agent.questionbank;

import com._202510007517.platform.agent.config.QuestionBankProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class QuestionRagService {
    private static final String NO_MATCH_MESSAGE = "题库暂无匹配题目，请先维护题库或调整主题/难度";

    private final QuestionBankProperties properties;
    private final EmbeddingClient embeddingClient;
    private final QuestionBankIndex index;
    private final Random random;

    public QuestionRagService(QuestionBankProperties properties,
                              EmbeddingClient embeddingClient,
                              QuestionBankIndex index) {
        this(properties, embeddingClient, index, new Random());
    }

    QuestionRagService(QuestionBankProperties properties,
                       EmbeddingClient embeddingClient,
                       QuestionBankIndex index,
                       Random random) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.index = index;
        this.random = random == null ? new Random() : random;
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
        String normalizedTopic = normalizeTopic(topic);
        if (!properties.isEnabled()) {
            return result(normalizedTopic, requestedCount, 0, false, normalizedDifficulty, List.of(), "题库题目生成功能未启用。");
        }

        String queryText = buildQueryText(normalizedTopic, normalizedDifficulty, type);
        List<QuestionChunk> candidates;
        if (normalizedTopic == null) {
            candidates = randomize(index.filter(
                    userRole,
                    null,
                    normalizedDifficulty,
                    type,
                    properties.getMaxCandidates()));
        } else {
            try {
                List<Double> queryVector = embeddingClient.embed(queryText);
                candidates = index.search(
                        queryVector,
                        userRole,
                        normalizedTopic,
                        normalizedDifficulty,
                        type,
                        properties.getMaxCandidates(),
                        properties.getMinScore());
            } catch (RuntimeException ex) {
                candidates = index.filter(
                        userRole,
                        normalizedTopic,
                        normalizedDifficulty,
                        type,
                        properties.getMaxCandidates());
            }
        }
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
        return result(normalizedTopic,
                requestedCount,
                actualCount,
                partial,
                normalizedDifficulty,
                questions,
                message);
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

    private String normalizeTopic(String topic) {
        String text = Objects.toString(topic, "").trim();
        if (text.isEmpty() || isGenericTopic(text) || isOnlyPunctuation(text)) {
            return null;
        }
        return text;
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

    private boolean isGenericTopic(String topic) {
        String normalized = topic.trim()
                .replace("随机题目", "")
                .replace("随机", "")
                .replace("基于刚才内容", "")
                .replace("基于当前内容", "")
                .replace("基于上述内容", "")
                .replace("基于前面内容", "")
                .replace("基于刚才", "")
                .replace("基于当前", "")
                .replace("基于上述", "")
                .replace("基于前面", "")
                .replace("刚才内容", "")
                .replace("当前内容", "")
                .replace("上述内容", "")
                .replace("前面内容", "")
                .replace("刚才", "")
                .replace("当前", "")
                .replace("上述", "")
                .replace("前面", "")
                .replace("基于", "")
                .replace("课堂练习题", "")
                .replace("课堂练习", "")
                .replace("练习题", "")
                .replace("练习", "")
                .replace("题库", "")
                .replace("题目", "")
                .replace("题", "")
                .replace("课程", "")
                .replace("综合练习", "")
                .replace("综合", "")
                .replaceAll("\\s+", "");
        return normalized.isBlank();
    }

    private boolean isOnlyPunctuation(String topic) {
        return topic != null && topic.trim().matches("^[\\p{Punct}“”‘’\"'`·、，。！？；：（）【】《》〈〉…—-]+$");
    }

    private List<QuestionChunk> randomize(List<QuestionChunk> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return candidates == null ? List.of() : candidates;
        }
        List<QuestionChunk> shuffled = new ArrayList<>(candidates);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            Collections.swap(shuffled, i, swapIndex);
        }
        return shuffled;
    }

    @FunctionalInterface
    public interface EmbeddingClient {
        List<Double> embed(String text);
    }
}
