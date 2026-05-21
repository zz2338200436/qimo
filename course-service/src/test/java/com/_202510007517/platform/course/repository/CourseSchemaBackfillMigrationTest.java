package com._202510007517.platform.course.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CourseSchemaBackfillMigrationTest {

    @Test
    void v1MigrationDoesNotOwnTeacherKnowledgePointsAfterItHasBeenApplied() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__init_course_schema.sql");
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql).doesNotContain("teacher_knowledge_points");
    }

    @Test
    void v21MigrationBackfillsTeacherKnowledgePointsForExistingDatabases() throws Exception {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V21__backfill_teacher_knowledge_points.sql");

        assertThat(migration.exists()).isTrue();
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create table if not exists teacher_knowledge_points");
        assertThat(sql)
                .contains("point_name")
                .contains("course_id")
                .contains("idx_teacher_knowledge_points_course");
    }
}
