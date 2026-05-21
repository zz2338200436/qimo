package com._202510007517.platform.common.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventInfrastructureMigrationTest {

    @Test
    void migration_creates_outbox_and_processed_event_tables() throws IOException {
        String sql = readMigration();

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS outbox_event"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS processed_event"));
        assertTrue(sql.contains("UNIQUE KEY uk_outbox_event_event_id"));
        assertTrue(sql.contains("UNIQUE KEY uk_processed_event_consumer"));
        assertTrue(sql.contains("KEY idx_outbox_event_status_next"));
    }

    private String readMigration() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V20_2__create_event_infrastructure.sql")) {
            assertNotNull(stream, "event infrastructure migration must be packaged in common");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
