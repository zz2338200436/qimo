package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.AnalysisServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AnalysisServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.config.enabled=false"
        })
class JpaAnalysisRepositoryTest {

    private static final String DATABASE_NAME = "analysis-repository-jpa-" + UUID.randomUUID();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnalysisRepository repository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.config.import", () -> "");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("eureka.client.register-with-eureka", () -> "false");
        registry.add("eureka.client.fetch-registry", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.cloud.stream.enabled", () -> "false");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach
    void setUp() {
        recreateSchema();
        if (repository instanceof JpaAnalysisRepository jpaRepository) {
            jpaRepository.setClock(Clock.fixed(Instant.parse("2026-05-20T08:00:00Z"), ZoneId.of("UTC")));
        }
    }

    @Test
    void listsDailyStudentStudyTimeFromAnalysisReadModel() {
        assertThat(AopUtils.getTargetClass(repository).getSimpleName()).isEqualTo("JpaAnalysisRepository");

        seedScoreTrend(11, 42, 2, "assignment", "2026-05-18 10:00:00");
        seedScoreTrend(12, 42, 2, "exam", "2026-05-19 10:00:00");
        seedScoreTrend(13, 42, 3, "exam", "2026-05-19 10:00:00");
        seedScoreTrend(14, 43, 2, "assignment", "2026-05-19 10:00:00");
        seedKnowledgeMastery(21, 42, 2, 2, "2026-05-19 12:00:00");

        List<Map<String, Object>> rows = repository.listStudentStudyTimeDistribution(
                42L,
                "daily",
                null,
                2L,
                null);

        assertThat(rows).hasSize(7);
        assertThat(row(rows, "2026-05-18"))
                .containsEntry("label", "05-18")
                .containsEntry("study_time", 1.0)
                .containsEntry("hours", 1.0);
        assertThat(row(rows, "2026-05-19"))
                .containsEntry("label", "05-19")
                .containsEntry("study_time", 3.0)
                .containsEntry("hours", 3.0);
        assertThat(row(rows, "2026-05-20"))
                .containsEntry("study_time", 0.0)
                .containsEntry("hours", 0.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsStudentLearningStatsFromAnalysisReadModel() {
        seedScoreTrend(11, 42, 2, "assignment", 80, "2026-05-18 10:00:00");
        seedScoreTrend(12, 42, 2, "exam", 90, "2026-05-19 10:00:00");
        seedScoreTrend(13, 42, 3, "exam", 60, "2026-05-19 10:00:00");
        seedScoreTrend(14, 43, 2, "assignment", 100, "2026-05-19 10:00:00");
        seedKnowledgeMastery(21, 42, 2, 2, "0.7500", "2026-05-19 12:00:00");
        seedKnowledgeMastery(22, 42, 3, 5, "0.5000", "2026-05-19 12:00:00");

        Map<String, Object> stats = repository.getStudentLearningStats(
                42L,
                "2025-2026-1",
                2L,
                "month");

        assertThat(stats)
                .containsEntry("studyTime", 4.0)
                .containsEntry("studyTimeChange", 0)
                .containsEntry("completedTasks", 2)
                .containsEntry("completedTasksChange", 0)
                .containsEntry("averageScore", 85.0)
                .containsEntry("averageScoreChange", 0.0)
                .containsEntry("knowledgeMastery", 75.0)
                .containsEntry("knowledgeMasteryChange", 0.0);
        assertThat((List<Double>) stats.get("studyTimeDistribution"))
                .containsExactly(0.0, 0.0, 0.0, 0.0, 1.0, 3.0, 0.0);
        assertThat((List<Map<String, Object>>) stats.get("knowledgePoints"))
                .singleElement()
                .satisfies(point -> assertThat(point)
                        .containsEntry("id", 2L)
                        .containsEntry("courseId", 2L)
                        .containsEntry("mastery", 75.0)
                        .containsEntry("practiceCount", 2));
        assertThat((Map<String, Object>) stats.get("studyPlan"))
                .containsEntry("completionPercentage", 0);
    }

    @Test
    void listsStudentKnowledgePointsFromAnalysisReadModel() {
        seedKnowledgeMastery(21, 42, 2, 201L, 3, "0.7500", "2026-05-19 12:00:00");
        seedKnowledgeMastery(22, 42, 3, 1, "0.5000", "2026-05-19 13:00:00");
        seedKnowledgeMastery(23, 43, 2, 5, "0.9000", "2026-05-19 14:00:00");

        List<Map<String, Object>> rows = repository.listStudentKnowledgePoints(
                42L,
                "2025-2026-1",
                2L,
                "month");

        assertThat(rows)
                .singleElement()
                .satisfies(point -> assertThat(point)
                        .containsEntry("id", 201L)
                        .containsEntry("courseId", 2L)
                        .containsEntry("name", "知识点 201")
                        .containsEntry("pointName", "知识点 201")
                        .containsEntry("courseName", "课程 2")
                        .containsEntry("description", "知识点 201 的掌握汇总")
                        .containsEntry("mastery", 75.0)
                        .containsEntry("practiceCount", 3));
    }

    @Test
    void getsStudentKnowledgePointDetailFromAnalysisReadModel() {
        seedKnowledgeMastery(21, 42, 2, 201L, 5, "0.9000", "2026-05-19 12:00:00");

        Map<String, Object> point = repository.getStudentKnowledgePointDetail(42L, 201L);

        assertThat(point)
                .containsEntry("id", 201L)
                .containsEntry("courseId", 2L)
                .containsEntry("name", "知识点 201")
                .containsEntry("mastery", 90.0)
                .containsEntry("practiceCount", 5)
                .containsEntry("difficulty", "中等");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsTeacherDashboardFromAnalysisReadModel() {
        seedScoreTrend(11, 42, 2, 1, "assignment", 80, "2026-05-18 10:00:00");
        seedScoreTrend(12, 43, 2, 1, "exam", 90, "2026-05-19 10:00:00");
        seedScoreTrend(13, 44, 3, 2, "exam", 60, "2026-05-19 11:00:00");
        seedKnowledgeMastery(21, 42, 2, 1, 2, "0.7500", "2026-05-19 12:00:00");

        Map<String, Object> dashboard = repository.getTeacherDashboard(7L, 1L, 2L, "month");

        assertThat(dashboard)
                .containsEntry("totalCourses", 1)
                .containsEntry("totalStudents", 2)
                .containsEntry("pendingAssignments", 0)
                .containsEntry("pendingExams", 0)
                .containsEntry("missingSubmissions", 0)
                .containsEntry("warningCount", 0)
                .containsEntry("overallProgress", 75.0);
        assertThat((List<String>) dashboard.get("courseNames")).containsExactly("课程 2");
        assertThat((List<Double>) dashboard.get("averageScores")).containsExactly(85.0);
        assertThat((List<Map<String, Object>>) dashboard.get("recentActivities"))
                .first()
                .satisfies(activity -> assertThat(activity)
                        .containsEntry("activityType", "成绩记录")
                        .containsEntry("studentId", 43L)
                        .containsEntry("studentName", "学生 43"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsTeacherLearningSummaryFromAnalysisReadModel() {
        seedScoreTrend(11, 42, 2, 1, "assignment", 80, "2026-05-18 10:00:00");
        seedScoreTrend(12, 42, 2, 1, "exam", 90, "2026-05-19 10:00:00");
        seedScoreTrend(13, 43, 2, 1, "exam", 60, "2026-05-19 11:00:00");
        seedKnowledgeMastery(21, 42, 2, 1, 2, "0.7500", "2026-05-19 12:00:00");
        seedKnowledgeMastery(22, 43, 2, 1, 1, "0.4000", "2026-05-19 13:00:00");

        Map<String, Object> summary = repository.getTeacherLearningSummary(7L, 1L, 2L, "month");

        assertThat(summary)
                .containsEntry("totalStudents", 2)
                .containsEntry("averageScore", 72.5)
                .containsEntry("totalPendingAssignments", 0)
                .containsEntry("overallProgress", 57.5);
        assertThat((List<Map<String, Object>>) summary.get("studentPerformances"))
                .hasSize(2)
                .first()
                .satisfies(student -> assertThat(student)
                        .containsEntry("studentId", 42L)
                        .containsEntry("realName", "学生 42")
                        .containsEntry("className", "班级 1")
                        .containsEntry("courseName", "课程 2")
                        .containsEntry("averageScore", 85)
                        .containsEntry("overallProgress", 75)
                        .containsEntry("status", "良好"));
    }

    @Test
    void listsKnowledgeMasteryByClassAndCourseScope() {
        seedKnowledgeMastery(21, 42, 2, 1, 2, "0.7500", "2026-05-19 12:00:00");
        seedKnowledgeMastery(22, 43, 2, 1, 1, "0.4000", "2026-05-19 13:00:00");
        seedKnowledgeMastery(23, 44, 2, 3, 5, "0.9000", "2026-05-19 14:00:00");

        assertThat(repository.listKnowledgeMasteryByScope(1L, 2L))
                .extracting(row -> row.studentId() + ":" + row.masteryScore())
                .containsExactly("43:0.4000", "42:0.7500");
    }

    @Test
    void upsertsKnowledgeMasterySeparatelyForEachKnowledgePoint() {
        repository.upsertKnowledgeMastery(new KnowledgeMasteryRecord(
                42L,
                2L,
                1L,
                501L,
                new BigDecimal("0.8000"),
                1,
                "exam",
                77L,
                "event-501-1",
                Instant.parse("2026-05-19T12:00:00Z")));
        repository.upsertKnowledgeMastery(new KnowledgeMasteryRecord(
                42L,
                2L,
                1L,
                502L,
                new BigDecimal("0.4000"),
                1,
                "assignment",
                88L,
                "event-502-1",
                Instant.parse("2026-05-19T13:00:00Z")));
        repository.upsertKnowledgeMastery(new KnowledgeMasteryRecord(
                42L,
                2L,
                1L,
                501L,
                new BigDecimal("0.6000"),
                1,
                "exam",
                78L,
                "event-501-2",
                Instant.parse("2026-05-19T14:00:00Z")));

        assertThat(repository.listKnowledgeMastery(42L, 2L))
                .extracting(row -> row.knowledgePointId() + ":" + row.masteryScore() + ":" + row.evidenceCount())
                .containsExactly("501:0.7000:2", "502:0.4000:1");
    }

    private static Map<String, Object> row(List<Map<String, Object>> rows, String date) {
        return rows.stream()
                .filter(row -> date.equals(row.get("date")))
                .findFirst()
                .orElseThrow();
    }

    private void seedScoreTrend(long id, long studentId, long courseId, String sourceType, String occurredAt) {
        seedScoreTrend(id, studentId, courseId, sourceType, 80, occurredAt);
    }

    private void seedScoreTrend(
            long id,
            long studentId,
            long courseId,
            String sourceType,
            int score,
            String occurredAt) {
        seedScoreTrend(id, studentId, courseId, null, sourceType, score, occurredAt);
    }

    private void seedScoreTrend(
            long id,
            long studentId,
            long courseId,
            Integer classId,
            String sourceType,
            int score,
            String occurredAt) {
        jdbcTemplate.update("""
                        INSERT INTO score_trends (
                            id, student_id, course_id, class_id, source_type, source_id, submission_id,
                            score, max_score, score_rate, occurred_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 100, ? / 100.0, ?)
                        """,
                id,
                studentId,
                courseId,
                classId,
                sourceType,
                id + 100,
                id + 1000,
                score,
                score,
                occurredAt);
    }

    private void seedKnowledgeMastery(
            long id,
            long studentId,
            long courseId,
            int evidenceCount,
            String updatedAt) {
        seedKnowledgeMastery(id, studentId, courseId, evidenceCount, "0.7500", updatedAt);
    }

    private void seedKnowledgeMastery(
            long id,
            long studentId,
            long courseId,
            int evidenceCount,
            String masteryScore,
            String updatedAt) {
        seedKnowledgeMastery(id, studentId, courseId, (Integer) null, evidenceCount, masteryScore, updatedAt);
    }

    private void seedKnowledgeMastery(
            long id,
            long studentId,
            long courseId,
            Integer classId,
            int evidenceCount,
            String masteryScore,
            String updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO kp_mastery (
                            id, student_id, course_id, class_id, mastery_score, evidence_count, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                studentId,
                courseId,
                classId,
                masteryScore,
                evidenceCount,
                updatedAt);
    }

    private void seedKnowledgeMastery(
            long id,
            long studentId,
            long courseId,
            Long knowledgePointId,
            int evidenceCount,
            String masteryScore,
            String updatedAt) {
        jdbcTemplate.update("""
                        INSERT INTO kp_mastery (
                            id, student_id, course_id, knowledge_point_id, mastery_score, evidence_count, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                studentId,
                courseId,
                knowledgePointId,
                masteryScore,
                evidenceCount,
                updatedAt);
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS score_trends");
        jdbcTemplate.execute("DROP TABLE IF EXISTS kp_mastery");
        jdbcTemplate.execute("""
                CREATE TABLE score_trends (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    class_id BIGINT NULL,
                    source_type VARCHAR(50) NOT NULL,
                    source_id BIGINT NOT NULL,
                    submission_id BIGINT NOT NULL,
                    score INT NULL,
                    max_score INT NULL,
                    score_rate DECIMAL(6,4) NULL,
                    occurred_at TIMESTAMP(6) NOT NULL,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE kp_mastery (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    class_id BIGINT NULL,
                    knowledge_point_id BIGINT NOT NULL DEFAULT 0,
                    mastery_score DECIMAL(6,4) NOT NULL,
                    evidence_count INT NOT NULL DEFAULT 0,
                    last_source_type VARCHAR(50) NULL,
                    last_source_id BIGINT NULL,
                    last_event_id VARCHAR(150) NULL,
                    updated_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT uk_kp_mastery_student_course_point UNIQUE (student_id, course_id, knowledge_point_id)
                )
                """);
    }
}
