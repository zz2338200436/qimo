package com._202510007517.platform.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLXML;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AiQuestionBankQueryService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiQuestionBankQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> summary() {
        try {
            List<Map<String, Object>> topics = jdbcTemplate.queryForList("""
                    SELECT kp.point_name AS knowledgePoint,
                           COALESCE(kp.difficulty, q.difficulty, '未标注') AS difficulty,
                           COUNT(q.id) AS questionCount
                    FROM knowledge_points kp
                    LEFT JOIN questions q ON q.knowledge_point_id = kp.id
                    GROUP BY kp.point_name, COALESCE(kp.difficulty, q.difficulty, '未标注')
                    HAVING COUNT(q.id) > 0
                    ORDER BY MIN(kp.order_index), MIN(kp.id), difficulty
                    """);
            List<Map<String, Object>> questions = jdbcTemplate.queryForList("""
                    SELECT q.id,
                           q.content,
                           q.correct_answer,
                           q.difficulty,
                           q.options,
                           q.type,
                           q.analysis,
                           kp.point_name
                    FROM questions q
                    JOIN knowledge_points kp ON kp.id = q.knowledge_point_id
                    ORDER BY kp.order_index, kp.id, q.id
                    """).stream().map(this::toQuestionPayload).toList();
            Number totalQuestions = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM questions", Number.class);
            Number totalKnowledgePoints = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(DISTINCT kp.point_name)
                            FROM questions q
                            JOIN knowledge_points kp ON kp.id = q.knowledge_point_id
                            """, Number.class);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalKnowledgePoints", totalKnowledgePoints == null ? 0 : totalKnowledgePoints.longValue());
            result.put("totalQuestions", totalQuestions == null ? 0 : totalQuestions.longValue());
            result.put("topics", topics);
            result.put("questions", questions);
            return result;
        } catch (DataAccessException ex) {
            return Map.of(
                    "totalKnowledgePoints", 0,
                    "totalQuestions", 0,
                    "topics", List.of(),
                    "questions", List.of(),
                    "message", "题库暂无可查询数据"
            );
        }
    }

    private Map<String, Object> toQuestionPayload(Map<String, Object> row) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("id", row.get("id"));
        question.put("content", row.get("content"));
        question.put("difficulty", row.get("difficulty"));
        question.put("type", displayQuestionType(row.get("type")));
        question.put("score", null);
        question.put("options", parseOptions(row.get("options")));
        question.put("answer", row.get("correct_answer"));
        question.put("analysis", row.get("analysis"));
        question.put("knowledgePoints", List.of(Objects.toString(row.get("point_name"), "")));
        question.put("sourcePath", null);
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
                return Arrays.stream(array).map(Objects::toString).toList();
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
}
