package com._202510007517.platform.notification.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Repository
public class JdbcNotificationRepository implements NotificationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcNotificationRepository(@Nullable NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public NotificationEntity save(NotificationEntity notification) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("studentId", notification.getStudentId())
                .addValue("teacherId", notification.getTeacherId())
                .addValue("type", notification.getType())
                .addValue("title", notification.getTitle())
                .addValue("content", notification.getContent())
                .addValue("relatedId", notification.getRelatedId())
                .addValue("isRead", notification.getRead() != null && notification.getRead());
        jdbcTemplate.update("""
                INSERT INTO notifications (
                    student_id,
                    teacher_id,
                    type,
                    title,
                    content,
                    related_id,
                    is_read
                ) VALUES (
                    :studentId,
                    :teacherId,
                    :type,
                    :title,
                    :content,
                    :relatedId,
                    :isRead
                )
                """, parameters, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key != null) {
            notification.setId(key.longValue());
        }
        return notification;
    }

    @Override
    public int markAsRead(Long studentId, Long notificationId) {
        return jdbcTemplate.update("""
                UPDATE notifications
                SET is_read = true
                WHERE id = :notificationId
                  AND student_id = :studentId
                """, new MapSqlParameterSource()
                .addValue("notificationId", notificationId)
                .addValue("studentId", studentId));
    }

    @Override
    public int markAllAsRead(Long studentId) {
        return jdbcTemplate.update("""
                UPDATE notifications
                SET is_read = true
                WHERE student_id = :studentId
                  AND is_read = false
                """, new MapSqlParameterSource("studentId", studentId));
    }

    @Override
    public int deleteByIdAndStudentId(Long studentId, Long notificationId) {
        return jdbcTemplate.update("""
                DELETE FROM notifications
                WHERE id = :notificationId
                  AND student_id = :studentId
                """, new MapSqlParameterSource()
                .addValue("notificationId", notificationId)
                .addValue("studentId", studentId));
    }

    @Override
    public int deleteAllReadByStudentId(Long studentId) {
        return jdbcTemplate.update("""
                DELETE FROM notifications
                WHERE student_id = :studentId
                  AND is_read = true
                """, new MapSqlParameterSource("studentId", studentId));
    }

    @Override
    public List<NotificationEntity> findStudentNotifications(Long studentId, int offset, int limit, String filter) {
        String sql = """
                SELECT id, student_id, teacher_id, type, title, content, related_id, is_read, created_at
                FROM notifications
                WHERE student_id = :studentId
                  AND (
                      :filter = 'all'
                      OR (:filter = 'read' AND is_read = true)
                      OR (:filter = 'unread' AND is_read = false)
                      OR type = :filter
                  )
                ORDER BY created_at DESC, id DESC
                LIMIT :limit OFFSET :offset
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("filter", normalizeFilter(filter))
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> mapRow(rs));
    }

    @Override
    public long countStudentNotifications(Long studentId, String filter) {
        String sql = """
                SELECT COUNT(1)
                FROM notifications
                WHERE student_id = :studentId
                  AND (
                      :filter = 'all'
                      OR (:filter = 'read' AND is_read = true)
                      OR (:filter = 'unread' AND is_read = false)
                      OR type = :filter
                  )
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("filter", normalizeFilter(filter));
        Long count = jdbcTemplate.queryForObject(sql, parameters, Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public List<NotificationEntity> findAllStudentNotifications(Long studentId, String filter) {
        String sql = """
                SELECT id, student_id, teacher_id, type, title, content, related_id, is_read, created_at
                FROM notifications
                WHERE student_id = :studentId
                  AND (
                      :filter = 'all'
                      OR (:filter = 'read' AND is_read = true)
                      OR (:filter = 'unread' AND is_read = false)
                      OR type = :filter
                  )
                ORDER BY created_at DESC, id DESC
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("filter", normalizeFilter(filter));
        return jdbcTemplate.query(sql, parameters, (rs, rowNum) -> mapRow(rs));
    }

    @Override
    public int countUnreadByStudentId(Long studentId) {
        String sql = """
                SELECT COUNT(1)
                FROM notifications
                WHERE student_id = :studentId
                  AND is_read = false
                """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("studentId", studentId), Integer.class);
        return count == null ? 0 : count;
    }

    private String normalizeFilter(String filter) {
        return StringUtils.hasText(filter) ? filter.trim().toLowerCase(Locale.ROOT) : "all";
    }

    private NotificationEntity mapRow(ResultSet rs) throws SQLException {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(rs.getLong("id"));
        entity.setStudentId(rs.getLong("student_id"));
        entity.setTeacherId((Long) rs.getObject("teacher_id"));
        entity.setType(rs.getString("type"));
        entity.setTitle(rs.getString("title"));
        entity.setContent(rs.getString("content"));
        entity.setRelatedId(rs.getLong("related_id"));
        if (rs.wasNull()) {
            entity.setRelatedId(null);
        }
        entity.setRead(rs.getBoolean("is_read"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        entity.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return entity;
    }
}
