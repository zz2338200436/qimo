package com._202510007517.platform.legacy.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LegacyKnowledgeReadService {

    private final JdbcTemplate jdbcTemplate;

    public LegacyKnowledgeReadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listKnowledgePoints(Long teacherId, Long courseId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT kp.id, kp.point_name, kp.course_id, kp.difficulty, kp.order_index,
                       c.course_name
                FROM knowledge_points kp
                JOIN courses c ON c.id = kp.course_id
                WHERE c.teacher_id = ?
                """);
        args.add(teacherId);
        if (courseId != null) {
            sql.append(" AND kp.course_id = ?");
            args.add(courseId);
        }
        sql.append(" ORDER BY kp.course_id ASC, kp.order_index ASC, kp.id ASC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("pointName", rs.getString("point_name"));
            row.put("name", rs.getString("point_name"));
            row.put("courseId", rs.getLong("course_id"));
            row.put("courseName", rs.getString("course_name"));
            row.put("difficulty", rs.getString("difficulty"));
            row.put("orderIndex", rs.getInt("order_index"));
            return row;
        }, args.toArray());
    }
}
