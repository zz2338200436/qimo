package com._202510007517.platform.assignment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentSchemaBackfillMigrationTest {

    @Test
    void v1MigrationDoesNotOwnAssignmentKnowledgePointsAfterItHasBeenApplied() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__init_assignment_schema.sql");
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql).doesNotContain("assignment_knowledge_points");
    }

    @Test
    void v21MigrationBackfillsAssignmentKnowledgePointsForExistingDatabases() throws Exception {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V21__backfill_assignment_knowledge_points.sql");

        assertThat(migration.exists()).isTrue();
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create table if not exists assignment_knowledge_points");
        assertThat(sql)
                .contains("assignment_id")
                .contains("knowledge_point_id")
                .contains("idx_assignment_knowledge_points_point");
    }
}
