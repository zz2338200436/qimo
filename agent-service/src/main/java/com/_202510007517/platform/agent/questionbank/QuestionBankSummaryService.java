package com._202510007517.platform.agent.questionbank;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class QuestionBankSummaryService {
    private final Supplier<List<QuestionChunk>> questionsSupplier;

    public QuestionBankSummaryService(List<QuestionChunk> questions) {
        this(() -> List.copyOf(questions == null ? List.of() : questions));
    }

    public QuestionBankSummaryService(Supplier<List<QuestionChunk>> questionsSupplier) {
        this.questionsSupplier = questionsSupplier == null ? List::of : questionsSupplier;
    }

    public Map<String, Object> summary() {
        List<QuestionChunk> questions = List.copyOf(questionsSupplier.get() == null ? List.of() : questionsSupplier.get());
        Map<String, Long> difficultyBreakdown = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> difficultyLabel(question.difficulty()),
                        LinkedHashMap::new,
                        Collectors.counting()));

        Map<String, Map<String, Long>> topicDifficultyBreakdown = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> textOrDefault(question.topic(), "未标注"),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                question -> difficultyLabel(question.difficulty()),
                                LinkedHashMap::new,
                                Collectors.counting())));

        List<Map<String, Object>> topics = topicDifficultyBreakdown.entrySet().stream()
                .flatMap(entry -> entry.getValue().entrySet().stream()
                        .map(difficultyEntry -> topicEntry(entry.getKey(), difficultyEntry.getKey(), difficultyEntry.getValue())))
                .sorted(Comparator.comparing((Map<String, Object> map) -> String.valueOf(map.get("knowledgePoint")))
                        .thenComparing(map -> String.valueOf(map.get("difficulty"))))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalKnowledgePoints", topicDifficultyBreakdown.size());
        result.put("totalQuestions", questions.size());
        result.put("topicCount", topicDifficultyBreakdown.size());
        result.put("topics", topics);
        result.put("difficultyBreakdown", difficultyBreakdown);
        result.put("topicDifficultyBreakdown", topicDifficultyBreakdown);
        result.put("questions", questions.stream().map(this::toPayload).toList());
        if (questions.isEmpty()) {
            result.put("message", "题库暂无可查询数据");
        }
        return result;
    }

    private Map<String, Object> topicEntry(String topic, String difficulty, Long questionCount) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("knowledgePoint", topic);
        entry.put("topic", topic);
        entry.put("difficulty", difficulty);
        entry.put("questionCount", questionCount);
        return entry;
    }

    private String difficultyLabel(String difficulty) {
        String normalized = textOrDefault(difficulty, "未标注");
        return normalized;
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
        payload.put("knowledgePoints", List.of(textOrDefault(question.topic(), "未标注")));
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

    private String textOrDefault(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
