package com._202510007517.platform.ai.model;

import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class LocalMockAiModelClient implements AiModelClient {

    private static final String NO_MATCH_MESSAGE = "题库暂无匹配题目，请先维护题库或调整主题/难度";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public LocalMockAiModelClient(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> generateQuestions(GenerateQuestionsRequestDTO request) {
        String normalizedDifficulty = normalizeDifficulty(request.getDifficulty());
        List<Map<String, Object>> questions = findQuestionBankQuestions(request, normalizedDifficulty);

        Map<String, Object> result = new HashMap<>();
        result.put("topic", request.getTopic());
        result.put("count", request.getCount());
        result.put("difficulty", normalizedDifficulty);
        result.put("questions", questions);
        if (questions.isEmpty()) {
            result.put("message", NO_MATCH_MESSAGE);
        }
        return result;
    }

    @Override
    public Map<String, Object> generateExam(GenerateExamRequestDTO request) {
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            questions.add(choiceQuestion(i, request.getCourseName() + "选择题 " + i, request.getDifficulty(), 10));
        }
        for (int i = 6; i <= 8; i++) {
            questions.add(textQuestion(i, request.getCourseName() + "填空题 " + (i - 5), request.getDifficulty(), "填空题", 10));
        }
        for (int i = 9; i <= 10; i++) {
            questions.add(textQuestion(i, request.getCourseName() + "简答题 " + (i - 8), request.getDifficulty(), "简答题", 25));
        }

        Map<String, Object> exam = new HashMap<>();
        exam.put("title", request.getCourseName() + "模拟试卷");
        exam.put("courseName", request.getCourseName());
        exam.put("totalScore", request.getTotalScore());
        exam.put("duration", request.getDuration());
        exam.put("difficulty", request.getDifficulty());
        exam.put("questions", questions);
        return exam;
    }

    @Override
    public Map<String, Object> generateLearningSuggestions(Long targetStudentId,
                                                           LearningSuggestionRequestDTO request) {
        Map<String, Object> result = new HashMap<>();
        result.put("studentId", targetStudentId);
        result.put("suggestions", List.of(
                "建议加强函数概念的理解",
                "多做积分相关的练习题",
                "参加每周的学习小组讨论"));
        return result;
    }

    @Override
    public String modelName(String requestType) {
        if ("generate-questions".equals(requestType)) {
            return "question-bank";
        }
        return AiModelClient.super.modelName(requestType);
    }

    private List<Map<String, Object>> findQuestionBankQuestions(GenerateQuestionsRequestDTO request, String difficulty) {
        String topicLike = "%" + request.getTopic().trim() + "%";
        Integer limit = Math.max(1, request.getCount());

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT q.id,
                           q.content,
                           q.correct_answer,
                           q.difficulty,
                           q.options,
                           q.score,
                           q.type,
                           q.analysis,
                           kp.point_name
                    FROM questions q
                    JOIN knowledge_points kp ON kp.id = q.knowledge_point_id
                    WHERE (q.content LIKE ? OR kp.point_name LIKE ? OR kp.description LIKE ?)
                      AND (q.difficulty = ? OR (q.difficulty IS NULL AND kp.difficulty = ?))
                    ORDER BY q.id
                    LIMIT ?
                    """, topicLike, topicLike, topicLike, difficulty, difficulty, limit);
            return rows.stream()
                    .map(this::toQuestionPayload)
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private String normalizeDifficulty(String value) {
        String text = Objects.toString(value, "").trim();
        if (text.isBlank()) {
            return "中等";
        }
        text = text.replace("难度", "").trim();
        return switch (text.toLowerCase()) {
            case "easy", "简单", "入门", "低" -> "简单";
            case "medium", "中", "中等", "中级" -> "中等";
            case "hard", "困难", "高级", "高" -> "困难";
            default -> text;
        };
    }

    private Map<String, Object> toQuestionPayload(Map<String, Object> row) {
        Map<String, Object> question = new HashMap<>();
        question.put("id", row.get("id"));
        question.put("content", row.get("content"));
        question.put("difficulty", row.get("difficulty"));
        question.put("type", displayQuestionType(row.get("type")));
        question.put("score", row.get("score"));
        question.put("options", parseOptions(row.get("options")));
        question.put("answer", row.get("correct_answer"));
        question.put("analysis", row.get("analysis"));
        question.put("knowledgePoints", List.of(Objects.toString(row.get("point_name"), "")));
        return question;
    }

    private List<String> parseOptions(Object value) {
        if (value == null) {
            return List.of();
        }
        try {
            if (value instanceof String[] array) {
                return List.of(array);
            }
            if (value instanceof Object[] array) {
                return Arrays.stream(array)
                        .map(Objects::toString)
                        .toList();
            }
            if (value instanceof byte[] bytes) {
                return readOptionsJson(new String(bytes, StandardCharsets.UTF_8));
            }
            if (value instanceof Clob clob) {
                return readOptionsJson(clob.getSubString(1, Math.toIntExact(clob.length())));
            }
            if (value instanceof SQLXML sqlxml) {
                return readOptionsJson(sqlxml.getString());
            }
            return readOptionsJson(value.toString());
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> readOptionsJson(String raw) throws java.io.IOException {
        if (raw.startsWith("\"[") && raw.endsWith("]\"")) {
            raw = objectMapper.readValue(raw, String.class);
        }
        return objectMapper.readValue(raw, STRING_LIST);
    }

    private String displayQuestionType(Object value) {
        if (value == null) {
            return "题目";
        }
        return switch (value.toString()) {
            case "SINGLE_CHOICE" -> "选择题";
            case "MULTIPLE_CHOICE" -> "多选题";
            case "TRUE_FALSE" -> "判断题";
            case "FILL_BLANK" -> "填空题";
            case "SHORT_ANSWER" -> "简答题";
            case "ESSAY" -> "论述题";
            default -> value.toString();
        };
    }

    private Map<String, Object> choiceQuestion(int id, String content, String difficulty, int score) {
        Map<String, Object> question = new HashMap<>();
        question.put("id", id);
        question.put("content", content);
        question.put("difficulty", difficulty);
        question.put("type", "选择题");
        question.put("score", score);
        question.put("options", List.of("选项A", "选项B", "选项C", "选项D"));
        question.put("answer", "A");
        return question;
    }

    private Map<String, Object> textQuestion(int id, String content, String difficulty, String type, int score) {
        Map<String, Object> question = new HashMap<>();
        question.put("id", id);
        question.put("content", content);
        question.put("difficulty", difficulty);
        question.put("type", type);
        question.put("score", score);
        question.put("answer", "正确答案");
        return question;
    }
}
