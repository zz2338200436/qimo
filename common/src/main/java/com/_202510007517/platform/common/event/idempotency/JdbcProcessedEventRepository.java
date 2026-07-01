package com._202510007517.platform.common.event.idempotency;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

public class JdbcProcessedEventRepository implements ProcessedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProcessedEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean insertIfAbsent(ProcessedEventEntity event) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO processed_event (event_id, event_type, consumer_name, processed_at)
                            VALUES (?, ?, ?, ?)
                            """,
                    event.eventId(),
                    event.eventType(),
                    event.consumerName(),
                    Timestamp.from(event.processedAt()));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public void delete(String eventId, String consumerName) {
        jdbcTemplate.update("""
                        DELETE FROM processed_event
                        WHERE event_id = ? AND consumer_name = ?
                        """,
                eventId,
                consumerName);
    }
}
