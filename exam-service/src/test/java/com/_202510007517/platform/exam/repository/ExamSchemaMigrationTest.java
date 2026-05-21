package com._202510007517.platform.exam.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ExamSchemaMigrationTest {

    @Test
    void v1MigrationCreatesExamQuestionsTableForScExamOwnership() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__init_exam_schema.sql");
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create table if not exists exam_questions");
        assertThat(sql)
                .contains("question_text")
                .contains("question_type")
                .contains("options_json")
                .contains("correct_answer")
                .contains("knowledge_point_id")
                .contains("sort_order");
    }

    @Test
    void v21MigrationBackfillsQuestionAndKnowledgePointTablesForBaselinedDatabases() throws Exception {
        ClassPathResource migration = new ClassPathResource(
                "db/migration/V21__backfill_exam_questions_and_knowledge_points.sql");

        assertThat(migration.exists()).isTrue();
        String sql = new String(migration.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        assertThat(sql)
                .contains("create table if not exists exam_knowledge_points")
                .contains("create table if not exists exam_questions")
                .contains("idx_exam_questions_exam_order");
    }
}
