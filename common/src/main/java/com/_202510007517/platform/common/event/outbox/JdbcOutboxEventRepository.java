package com._202510007517.platform.common.event.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class JdbcOutboxEventRepository implements OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOutboxEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(OutboxEventEntity event) {
        jdbcTemplate.update("""
                        INSERT INTO outbox_event (
                            event_id, aggregate_type, aggregate_id, event_type, binding_name,
                            payload, headers, status, retry_count, next_retry_at, last_error,
                            created_at, updated_at, published_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.bindingName(),
                event.payload(),
                event.headers(),
                event.status(),
                event.retryCount(),
                Timestamp.from(event.nextRetryAt()),
                event.lastError(),
                Timestamp.from(event.createdAt()),
                Timestamp.from(event.updatedAt()),
                toTimestamp(event.publishedAt()));
    }

    @Override
    public List<OutboxEventEntity> findDuePendingEvents(int limit, Instant now) {
        return jdbcTemplate.query("""
                        SELECT id, event_id, aggregate_type, aggregate_id, event_type, binding_name,
                               payload, headers, status, retry_count, next_retry_at, last_error,
                               created_at, updated_at, published_at
                        FROM outbox_event
                        WHERE status = 0 AND next_retry_at <= ?
                        ORDER BY id
                        LIMIT ?
                        """,
                (rs, rowNum) -> mapRow(rs),
                Timestamp.from(now),
                limit);
    }

    @Override
    public void markPublished(long id, Instant publishedAt) {
        jdbcTemplate.update("""
                        UPDATE outbox_event
                        SET status = 1, published_at = ?, updated_at = ?, last_error = NULL
                        WHERE id = ?
                        """,
                Timestamp.from(publishedAt),
                Timestamp.from(publishedAt),
                id);
    }

    @Override
    public void markFailed(long id, int retryCount, Instant nextRetryAt, String lastError, int maxRetries) {
        int nextStatus = retryCount >= maxRetries ? 2 : 0;
        jdbcTemplate.update("""
                        UPDATE outbox_event
                        SET status = ?, retry_count = ?, next_retry_at = ?, last_error = ?, updated_at = ?
                        WHERE id = ?
                        """,
                nextStatus,
                retryCount,
                Timestamp.from(nextRetryAt),
                lastError,
                Timestamp.from(Instant.now()),
                id);
    }

    private OutboxEventEntity mapRow(ResultSet rs) throws SQLException {
        return new OutboxEventEntity(
                rs.getLong("id"),
                rs.getString("event_id"),
                rs.getString("aggregate_type"),
                rs.getString("aggregate_id"),
                rs.getString("event_type"),
                rs.getString("binding_name"),
                rs.getString("payload"),
                rs.getString("headers"),
                rs.getInt("status"),
                rs.getInt("retry_count"),
                toInstant(rs.getTimestamp("next_retry_at")),
                rs.getString("last_error"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                toInstant(rs.getTimestamp("published_at")));
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
