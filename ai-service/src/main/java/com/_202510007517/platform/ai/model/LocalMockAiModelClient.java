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
        int requestedCount = Math.max(1, request.getCount());
        int actualCount = questions.size();
        boolean partial = actualCount > 0 && actualCount < requestedCount;

        Map<String, Object> result = new HashMap<>();
        result.put("topic", request.getTopic());
        result.put("count", requestedCount);
        result.put("actualCount", actualCount);
        result.put("partial", partial);
        result.put("difficulty", normalizedDifficulty);
        result.put("questions", questions);
        if (questions.isEmpty()) {
            result.put("message", NO_MATCH_MESSAGE);
        } else if (partial) {
            result.put("message", "题库仅匹配到 " + actualCount + "/" + requestedCount + " 道题，请补充题库或放宽主题/难度条件");
        }
        return result;
    }

    @Override
    public Map<String, Object> generateExam(GenerateExamRequestDTO request) {
        String normalizedDifficulty = normalizeDifficulty(request.getDifficulty());

        // 优先从题库中匹配真实题目
        List<Map<String, Object>> bankQuestions = findExamQuestionsFromBank(
                request.getCourseName(), normalizedDifficulty);

        Map<String, Object> exam = new HashMap<>();
        exam.put("title", request.getCourseName() + " 模拟试卷");
        exam.put("courseName", request.getCourseName());
        exam.put("totalScore", request.getTotalScore());
        exam.put("duration", request.getDuration());
        exam.put("difficulty", normalizedDifficulty);

        if (!bankQuestions.isEmpty()) {
            exam.put("questions", bankQuestions);
            exam.put("source", "question-bank");
        } else {
            // 题库无匹配题目时给出明确提示，不再生成占位题目
            exam.put("questions", List.of());
            exam.put("message", "题库暂无「" + request.getCourseName() + "」相关题目，请先维护题库或联系管理员。");
        }
        return exam;
    }

    /** 从题库按课程名/主题查询适合组成试卷的真实题目 */
    private List<Map<String, Object>> findExamQuestionsFromBank(String courseName, String difficulty) {
        String topicLike = "%" + courseName.trim() + "%";
        // 一次查询获取尽可能多的匹配题目（上限30道），前端按类型自行分配
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
                    ORDER BY RAND()
                    LIMIT 30
                    """, topicLike, topicLike, topicLike, difficulty, difficulty);
            return rows.stream()
                    .map(this::toQuestionPayload)
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
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
        Integer limit = Math.max(1, request.getCount());
        String topic = Objects.toString(request.getTopic(), "").trim();

        try {
            List<Map<String, Object>> rows;
            if (topic.isBlank()) {
                rows = jdbcTemplate.queryForList("""
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
                        WHERE (q.difficulty = ? OR (q.difficulty IS NULL AND kp.difficulty = ?))
                        ORDER BY RAND()
                        LIMIT ?
                        """, difficulty, difficulty, limit);
            } else {
                String topicLike = "%" + topic + "%";
                rows = jdbcTemplate.queryForList("""
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
                        ORDER BY RAND()
                        LIMIT ?
                        """, topicLike, topicLike, topicLike, difficulty, difficulty, limit);
            }
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
