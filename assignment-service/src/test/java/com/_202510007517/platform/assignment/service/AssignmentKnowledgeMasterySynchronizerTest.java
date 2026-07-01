package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentKnowledgeMasterySynchronizerTest {

    private JdbcTemplate jdbcTemplate;
    private AssignmentKnowledgeMasterySynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("""
                jdbc:h2:mem:assignment-mastery;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;\
                INIT=CREATE SCHEMA IF NOT EXISTS sc_course\\;CREATE SCHEMA IF NOT EXISTS sc_analysis\\;CREATE SCHEMA IF NOT EXISTS major_assignment
                """);
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        synchronizer = new AssignmentKnowledgeMasterySynchronizer(jdbcTemplate);
        recreateSchema();
    }

    @Test
    void courseKnowledgePointFallbackUsesAllGradedCourseAssignmentsForTheStudent() {
        seedCourseKnowledgePointFallbackData();

        synchronizer.syncAfterAssignmentGraded(assignment(99208L), submission(9920408L, 99208L, 42L, 89));
        synchronizer.syncAfterAssignmentGraded(assignment(99209L), submission(9920409L, 99209L, 42L, 78));

        BigDecimal masteryScore = jdbcTemplate.queryForObject("""
                SELECT mastery_score
                FROM sc_analysis.kp_mastery
                WHERE student_id = 42
                  AND course_id = 91005
                  AND knowledge_point_id = 174
                """, BigDecimal.class);
        String masteryLevel = jdbcTemplate.queryForObject("""
                SELECT mastery_level
                FROM major_assignment.knowledge_mastery
                WHERE student_id = 42
                  AND knowledge_point_id = 174
                """, String.class);

        assertThat(masteryScore).isEqualByComparingTo("0.8350");
        assertThat(masteryLevel).isEqualTo("良好");
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS sc_analysis.kp_mastery");
        jdbcTemplate.execute("DROP TABLE IF EXISTS major_assignment.knowledge_mastery");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sc_course.class_courses");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sc_course.class_students");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sc_course.teacher_knowledge_points");
        jdbcTemplate.execute("DROP TABLE IF EXISTS assignment_knowledge_points");
        jdbcTemplate.execute("DROP TABLE IF EXISTS assignment_submissions");
        jdbcTemplate.execute("DROP TABLE IF EXISTS assignments");

        jdbcTemplate.execute("""
                CREATE TABLE assignments (
                    id BIGINT PRIMARY KEY,
                    course_id BIGINT NOT NULL,
                    max_score INT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE assignment_submissions (
                    id BIGINT PRIMARY KEY,
                    assignment_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    graded BOOLEAN DEFAULT FALSE,
                    score INT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE assignment_knowledge_points (
                    assignment_id BIGINT NOT NULL,
                    knowledge_point_id BIGINT NOT NULL,
                    PRIMARY KEY (assignment_id, knowledge_point_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_course.teacher_knowledge_points (
                    id BIGINT PRIMARY KEY,
                    course_id BIGINT NOT NULL,
                    point_name VARCHAR(100),
                    order_index INT DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_course.class_students (
                    class_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_course.class_courses (
                    class_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE major_assignment.knowledge_mastery (
                    student_id BIGINT NOT NULL,
                    knowledge_point_id BIGINT NOT NULL,
                    mastery_level VARCHAR(20) NOT NULL,
                    last_assessed_date TIMESTAMP,
                    PRIMARY KEY (student_id, knowledge_point_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_analysis.kp_mastery (
                    student_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    class_id BIGINT,
                    knowledge_point_id BIGINT NOT NULL,
                    mastery_score DECIMAL(6,4) NOT NULL,
                    evidence_count INT NOT NULL,
                    last_source_type VARCHAR(30),
                    last_source_id BIGINT,
                    last_event_id VARCHAR(100),
                    updated_at TIMESTAMP,
                    PRIMARY KEY (student_id, course_id, knowledge_point_id)
                )
                """);
    }

    private void seedCourseKnowledgePointFallbackData() {
        jdbcTemplate.update("INSERT INTO assignments (id, course_id, max_score) VALUES (?, ?, ?)", 99208L, 91005L, 100);
        jdbcTemplate.update("INSERT INTO assignments (id, course_id, max_score) VALUES (?, ?, ?)", 99209L, 91005L, 100);
        jdbcTemplate.update("INSERT INTO assignment_submissions (id, assignment_id, student_id, graded, score) VALUES (?, ?, ?, ?, ?)",
                9920408L, 99208L, 42L, true, 89);
        jdbcTemplate.update("INSERT INTO assignment_submissions (id, assignment_id, student_id, graded, score) VALUES (?, ?, ?, ?, ?)",
                9920409L, 99209L, 42L, true, 78);
        jdbcTemplate.update("INSERT INTO sc_course.teacher_knowledge_points (id, course_id, point_name, order_index) VALUES (?, ?, ?, ?)",
                174L, 91005L, "云计算技术知识点1", 0);
        jdbcTemplate.update("INSERT INTO sc_course.class_students (class_id, student_id) VALUES (?, ?)", 90209L, 42L);
        jdbcTemplate.update("INSERT INTO sc_course.class_courses (class_id, course_id) VALUES (?, ?)", 90209L, 91005L);
    }

    private static AssignmentRecord assignment(Long id) {
        AssignmentRecord assignment = new AssignmentRecord();
        assignment.setId(id);
        assignment.setCourseId(91005L);
        assignment.setMaxScore(100);
        return assignment;
    }

    private static AssignmentSubmissionRecord submission(Long id, Long assignmentId, Long studentId, Integer score) {
        AssignmentSubmissionRecord submission = new AssignmentSubmissionRecord();
        submission.setId(id);
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(studentId);
        submission.setScore(score);
        submission.setGraded(true);
        return submission;
    }
}
