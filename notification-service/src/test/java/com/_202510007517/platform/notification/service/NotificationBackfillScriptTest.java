package com._202510007517.platform.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationBackfillScriptTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:notification-backfill;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        recreateSchemas();
        seedLegacyNotifications();
    }

    @Test
    void backfillScriptMigratesLegacyNotificationsAndCanBeRerun() throws Exception {
        Path script = findRepositoryRoot()
                .resolve("deploy/scripts/backfill/backfill-notification-history.sql");
        assertThat(Files.exists(script)).as("backfill script exists").isTrue();

        ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), new FileSystemResource(script));
        ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), new FileSystemResource(script));

        assertThat(count("sc_notification.notifications")).isEqualTo(2);

        Map<String, Object> first = jdbcTemplate.queryForMap("""
                SELECT student_id, teacher_id, type, title, content, related_id, is_read, created_at
                FROM sc_notification.notifications
                WHERE student_id = 42
                """);
        assertThat(first.get("student_id")).isEqualTo(42L);
        assertThat(first.get("teacher_id")).isEqualTo(7L);
        assertThat(first.get("type")).isEqualTo("course");
        assertThat(first.get("title")).isEqualTo("开课通知");
        assertThat(first.get("content")).isEqualTo("请同学按时参加第一节课");
        assertThat(first.get("related_id")).isEqualTo(5001L);
        assertThat(first.get("is_read")).isEqualTo(false);
        assertThat(((Timestamp) first.get("created_at")).toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 5, 19, 8, 0));

        Map<String, Object> second = jdbcTemplate.queryForMap("""
                SELECT student_id, teacher_id, type, title, content, related_id, is_read, created_at
                FROM sc_notification.notifications
                WHERE student_id = 43
                """);
        assertThat(second.get("teacher_id")).isNull();
        assertThat(second.get("type")).isEqualTo("warning");
        assertThat(second.get("title")).isEqualTo("学情预警");
        assertThat(second.get("related_id")).isNull();
        assertThat(second.get("is_read")).isEqualTo(true);
    }

    private long count(String tableName) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return value == null ? 0L : value;
    }

    private void recreateSchemas() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS major_assignment CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS sc_notification CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA major_assignment");
        jdbcTemplate.execute("CREATE SCHEMA sc_notification");
        jdbcTemplate.execute("""
                CREATE TABLE major_assignment.notifications (
                    id BIGINT NOT NULL PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    teacher_id BIGINT NULL,
                    type VARCHAR(50) NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    content TEXT NOT NULL,
                    related_id BIGINT NULL,
                    is_read BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_notification.notifications (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    teacher_id BIGINT NULL,
                    type VARCHAR(50) NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    content TEXT NOT NULL,
                    related_id BIGINT NULL,
                    is_read BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    CONSTRAINT uk_notifications_legacy_dedupe UNIQUE (
                        student_id,
                        teacher_id,
                        type,
                        title,
                        related_id,
                        created_at
                    )
                )
                """);
    }

    private void seedLegacyNotifications() {
        jdbcTemplate.update("""
                INSERT INTO major_assignment.notifications (
                    id, student_id, teacher_id, type, title, content, related_id, is_read, created_at
                ) VALUES
                    (1, 42, 7, 'course', '开课通知', '请同学按时参加第一节课', 5001, false, TIMESTAMP '2026-05-19 08:00:00'),
                    (2, 43, NULL, 'warning', '学情预警', '考试得分偏低，请及时复习', NULL, true, TIMESTAMP '2026-05-19 09:00:00')
                """);
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("deploy"))
                    && Files.isDirectory(current.resolve("notification-service"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
