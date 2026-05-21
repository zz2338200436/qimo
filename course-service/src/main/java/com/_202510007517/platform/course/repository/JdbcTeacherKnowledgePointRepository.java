package com._202510007517.platform.course.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcTeacherKnowledgePointRepository implements TeacherKnowledgePointRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTeacherKnowledgePointRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findByTeacherId(Long teacherId) {
        return jdbcTemplate.query("""
                SELECT kp.id AS id,
                       kp.point_name AS pointName,
                       kp.point_name AS name,
                       kp.description AS description,
                       kp.difficulty AS difficulty,
                       kp.order_index AS orderIndex,
                       kp.course_id AS courseId,
                       c.course_name AS courseName
                FROM teacher_knowledge_points kp
                JOIN courses c ON c.id = kp.course_id
                WHERE c.teacher_id = :teacherId
                ORDER BY kp.course_id ASC, kp.order_index ASC, kp.id ASC
                """, new MapSqlParameterSource("teacherId", teacherId), (rs, rowNum) -> toRow(rs));
    }

    @Override
    public List<Map<String, Object>> findByTeacherIdAndCourseId(Long teacherId, Long courseId) {
        return jdbcTemplate.query("""
                SELECT kp.id AS id,
                       kp.point_name AS pointName,
                       kp.point_name AS name,
                       kp.description AS description,
                       kp.difficulty AS difficulty,
                       kp.order_index AS orderIndex,
                       kp.course_id AS courseId,
                       c.course_name AS courseName
                FROM teacher_knowledge_points kp
                JOIN courses c ON c.id = kp.course_id
                WHERE c.teacher_id = :teacherId
                  AND kp.course_id = :courseId
                ORDER BY kp.order_index ASC, kp.id ASC
                """, new MapSqlParameterSource("teacherId", teacherId).addValue("courseId", courseId), (rs, rowNum) -> toRow(rs));
    }

    @Override
    public Optional<Map<String, Object>> findByTeacherIdAndKnowledgePointId(Long teacherId, Long knowledgePointId) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT kp.id AS id,
                       kp.point_name AS pointName,
                       kp.point_name AS name,
                       kp.description AS description,
                       kp.difficulty AS difficulty,
                       kp.order_index AS orderIndex,
                       kp.course_id AS courseId,
                       c.course_name AS courseName
                FROM teacher_knowledge_points kp
                JOIN courses c ON c.id = kp.course_id
                WHERE c.teacher_id = :teacherId
                  AND kp.id = :knowledgePointId
                """, new MapSqlParameterSource("teacherId", teacherId).addValue("knowledgePointId", knowledgePointId), (rs, rowNum) -> toRow(rs));
        return rows.stream().findFirst();
    }

    @Override
    public Long insert(Map<String, Object> payload) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO teacher_knowledge_points
                    (point_name, description, difficulty, order_index, course_id)
                VALUES
                    (:pointName, :description, :difficulty, :orderIndex, :courseId)
                """, new MapSqlParameterSource(payload), keyHolder, new String[]{"id"});
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    @Override
    public void update(Map<String, Object> payload) {
        jdbcTemplate.update("""
                UPDATE teacher_knowledge_points
                SET point_name = :pointName,
                    description = :description,
                    difficulty = :difficulty,
                    order_index = :orderIndex,
                    course_id = :courseId
                WHERE id = :id
                """, new MapSqlParameterSource(payload));
    }

    @Override
    public void delete(Long knowledgePointId) {
        jdbcTemplate.update("DELETE FROM teacher_knowledge_points WHERE id = :id",
                new MapSqlParameterSource("id", knowledgePointId));
    }

    private static Map<String, Object> toRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("pointName", rs.getString("pointName"));
        row.put("name", rs.getString("name"));
        row.put("description", rs.getString("description"));
        row.put("difficulty", rs.getString("difficulty"));
        row.put("orderIndex", rs.getInt("orderIndex"));
        row.put("courseId", rs.getLong("courseId"));
        row.put("courseName", rs.getString("courseName"));
        return row;
    }
}
