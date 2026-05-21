package com._202510007517.platform.analysis.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisSchemaBackfillMigrationTest {

    @Test
    void v1MigrationDoesNotOwnEarlyWarningsAfterItHasBeenApplied() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__init_analysis_schema.sql");
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql).doesNotContain("early_warnings");
    }

    @Test
    void v21MigrationBackfillsEarlyWarningsForExistingDatabases() throws Exception {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V21__backfill_early_warnings.sql");

        assertThat(migration.exists()).isTrue();
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create table if not exists early_warnings");
        assertThat(sql)
                .contains("student_id")
                .contains("teacher_id")
                .contains("warning_type")
                .contains("idx_early_warnings_teacher_course");
    }
}
