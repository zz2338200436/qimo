package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.controller.dto.EarlyWarningDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcEarlyWarningRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcEarlyWarningRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:early-warning-repository;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        recreateSchema();
        repository = new JdbcEarlyWarningRepository(jdbcTemplate);
    }

    @Test
    void filtersAndPagesTeacherWarningsWithLegacyComputedFields() {
        seedWarning(11, 7, 42, 2, 1, "LOW_SCORE", "HIGH", false);
        seedWarning(12, 7, 43, 2, 1, "LOW_ATTENDANCE", "MEDIUM", true);
        seedWarning(13, 8, 44, 2, 1, "LOW_SCORE", "HIGH", false);

        List<EarlyWarningDTO> warnings = repository.findWarningsByCondition(
                7L,
                1L,
                2L,
                "LOW_SCORE",
                "pending",
                0,
                10);

        assertThat(warnings).hasSize(1);
        EarlyWarningDTO warning = warnings.get(0);
        assertThat(warning.id()).isEqualTo(11L);
        assertThat(warning.status()).isEqualTo("pending");
        assertThat(warning.reason()).isEqualTo("阶段测验低于及格线");
        assertThat(warning.suggestion()).isEqualTo("建议安排课后辅导，重点讲解薄弱知识点");
        assertThat(repository.countWarningsByCondition(7L, 1L, 2L, "LOW_SCORE", "pending")).isEqualTo(1);
        assertThat(repository.countTotalWarnings(7L, 1L, 2L)).isEqualTo(2);
        assertThat(repository.countPendingWarnings(7L, 1L, 2L)).isEqualTo(1);
    }

    @Test
    void findsStudentWarningsNewestFirstWithoutLeakingOtherStudents() {
        seedWarning(11, 7, 42, 2, 1, "LOW_SCORE", "HIGH", false);
        seedWarning(12, 7, 43, 2, 1, "LOW_ATTENDANCE", "MEDIUM", false);
        seedWarning(13, 8, 42, 3, 1, "PROGRESS", "MEDIUM", true);

        List<EarlyWarningDTO> warnings = repository.findByStudentId(42L);

        assertThat(warnings).extracting(EarlyWarningDTO::id).containsExactly(13L, 11L);
        assertThat(warnings.get(0).status()).isEqualTo("resolved");
        assertThat(warnings.get(1).suggestion()).isEqualTo("建议安排课后辅导，重点讲解薄弱知识点");
    }

    @Test
    void updatesAndDeletesTeacherOwnedWarningsOnly() {
        seedWarning(11, 7, 42, 2, 1, "LOW_SCORE", "HIGH", false);
        seedWarning(12, 8, 43, 2, 1, "LOW_SCORE", "HIGH", false);

        boolean updated = repository.updateWarningStatus(7L, 11L, "resolved", "已电话沟通");
        boolean crossTeacherUpdated = repository.updateWarningStatus(7L, 12L, "resolved", "越权");

        assertThat(updated).isTrue();
        assertThat(crossTeacherUpdated).isFalse();
        EarlyWarningDTO warning = repository.findWarningById(7L, 11L);
        assertThat(warning.isResolved()).isTrue();
        assertThat(warning.status()).isEqualTo("resolved");
        assertThat(warning.resolvedBy()).isEqualTo(7L);
        assertThat(warning.resolvedNote()).isEqualTo("已电话沟通");

        assertThat(repository.deleteById(7L, 12L)).isFalse();
        assertThat(repository.deleteById(7L, 11L)).isTrue();
        assertThat(repository.findWarningById(7L, 11L)).isNull();
    }

    @Test
    void createsWarningWithLegacyFallbackNames() {
        EarlyWarningDTO created = repository.insert(
                7L,
                42L,
                2L,
                "LOW_SCORE",
                "HIGH",
                "阶段测验低于及格线",
                "exam",
                99L);

        assertThat(created.id()).isNotNull();
        assertThat(created.teacherId()).isEqualTo(7L);
        assertThat(created.studentName()).isEqualTo("学生 42");
        assertThat(created.courseName()).isEqualTo("课程 2");
        assertThat(repository.findWarningsForExport(7L, null, null, null, null)).hasSize(1);
    }

    @Test
    void detectsPendingWarningByTeacherStudentCourseAndType() {
        seedWarning(11, 7, 42, 2, 1, "LOW_SCORE", "HIGH", false);
        seedWarning(12, 7, 42, 2, 1, "PROGRESS", "MEDIUM", true);

        assertThat(repository.existsPendingWarning(7L, 42L, 2L, "LOW_SCORE")).isTrue();
        assertThat(repository.existsPendingWarning(7L, 42L, 2L, "PROGRESS")).isFalse();
        assertThat(repository.existsPendingWarning(8L, 42L, 2L, "LOW_SCORE")).isFalse();
    }

    private void seedWarning(
            long id,
            long teacherId,
            long studentId,
            long courseId,
            long classId,
            String type,
            String level,
            boolean resolved) {
        jdbcTemplate.update("""
                        INSERT INTO early_warnings (
                            id, student_id, course_id, class_id, teacher_id, warning_type, warning_level,
                            warning_message, trigger_date, is_resolved, student_name, course_name
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), ?, ?, ?)
                        """,
                id,
                studentId,
                courseId,
                classId,
                teacherId,
                type,
                level,
                "阶段测验低于及格线",
                resolved,
                "学生 " + studentId,
                "课程 " + courseId);
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS early_warnings");
        jdbcTemplate.execute("""
                CREATE TABLE early_warnings (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    class_id BIGINT NULL,
                    teacher_id BIGINT NOT NULL,
                    warning_type VARCHAR(50) NOT NULL,
                    warning_level VARCHAR(50) NOT NULL,
                    warning_message VARCHAR(1000) NOT NULL,
                    trigger_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
                    resolved_by BIGINT NULL,
                    resolved_date TIMESTAMP(6) NULL,
                    resolved_note VARCHAR(1000) NULL,
                    assessment_type VARCHAR(50) NULL,
                    related_assessment_id BIGINT NULL,
                    student_name VARCHAR(100) NULL,
                    course_name VARCHAR(100) NULL,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                )
                """);
    }
}
