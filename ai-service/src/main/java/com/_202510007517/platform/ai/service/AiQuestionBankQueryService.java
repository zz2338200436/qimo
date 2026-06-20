package com._202510007517.platform.ai.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiQuestionBankQueryService {

    private final JdbcTemplate jdbcTemplate;

    public AiQuestionBankQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
            return result;
        } catch (DataAccessException ex) {
            return Map.of(
                    "totalKnowledgePoints", 0,
                    "totalQuestions", 0,
                    "topics", List.of(),
                    "message", "题库暂无可查询数据"
            );
        }
    }
}
