package com._202510007517.platform.assignment.repository;

import com._202510007517.platform.assignment.AssignmentServiceApplication;
import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AssignmentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaAssignmentRepositoryTest {

    private static final String DATABASE_NAME = "assignment-jpa-" + UUID.randomUUID();

    @Autowired
    private AssignmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
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
        seedData();
    }

    @Test
    void findsAssignmentsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaAssignmentRepository.class);

        Optional<AssignmentRecord> loaded = repository.findById(101L);
        List<AssignmentRecord> courseAssignments = repository.findByCourseId(301L);
        List<AssignmentRecord> teacherAssignments = repository.findByTeacherId(7L);
        List<AssignmentRecord> studentAssignments = repository.findByStudentId(42L);
        List<AssignmentRecord> classAssignments = repository.findByClassIds(List.of(501L, 503L));

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getTitle()).isEqualTo("Distributed Systems Essay");
        assertThat(loaded.orElseThrow().getSubmissionCount()).isEqualTo(2);
        assertThat(loaded.orElseThrow().getGradedCount()).isEqualTo(1);
        assertThat(courseAssignments).extracting(AssignmentRecord::getId).containsExactly(101L);
        assertThat(teacherAssignments).extracting(AssignmentRecord::getId).containsExactly(102L, 101L);
        assertThat(studentAssignments).extracting(AssignmentRecord::getId).containsExactly(101L);
        assertThat(classAssignments).extracting(AssignmentRecord::getId).containsExactly(102L, 101L);
    }

    @Test
    void mutatesAssignmentsAndRelationsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaAssignmentRepository.class);

        AssignmentRecord assignment = new AssignmentRecord();
        assignment.setTitle("Operating Systems Lab");
        assignment.setDescription("Thread scheduling");
        assignment.setCourseId(303L);
        assignment.setDueDate("2026-12-01 23:59:59");
        assignment.setPublishDate("2026-11-01 08:00:00");
        assignment.setIsActive(true);
        assignment.setTeacherId(7L);
        assignment.setMaxScore(100);
        assignment.setSubmissionCount(0);
        assignment.setGradedCount(0);
        assignment.setStatus("pending");
        assignment.setTotalStudents(40);

        AssignmentRecord inserted = repository.insertAssignment(assignment);
        inserted.setTitle("Operating Systems Lab Updated");
        inserted.setDescription("Thread scheduling and locks");
        inserted.setTotalStudents(41);
        repository.updateAssignmentDetails(inserted);
        inserted.setSubmissionCount(1);
        inserted.setGradedCount(1);
        inserted.setStatus("graded");
        repository.updateAssignment(inserted);
        repository.replaceAssignmentClasses(inserted.getId(), List.of(501L, 501L, 502L));
        repository.replaceAssignmentKnowledgePoints(inserted.getId(), List.of(9002L, 9001L, 9002L));

        assertThat(inserted.getId()).isNotNull();
        AssignmentRecord loaded = repository.findById(inserted.getId()).orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("Operating Systems Lab Updated");
        assertThat(loaded.getStatus()).isEqualTo("graded");
        assertThat(loaded.getTotalStudents()).isEqualTo(41);
        assertThat(repository.findByClassIds(List.of(502L))).extracting(AssignmentRecord::getId).contains(inserted.getId());
        assertThat(repository.findKnowledgePointIdsByAssignmentId(inserted.getId())).containsExactly(9001L, 9002L);

        repository.deleteAssignmentCascade(inserted.getId());

        assertThat(repository.findById(inserted.getId())).isEmpty();
        assertThat(repository.findKnowledgePointIdsByAssignmentId(inserted.getId())).isEmpty();
        assertThat(repository.findByClassIds(List.of(501L, 502L))).extracting(AssignmentRecord::getId)
                .doesNotContain(inserted.getId());
    }

    @Test
    void mutatesSubmissionsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaAssignmentRepository.class);

        AssignmentSubmissionRecord submission = new AssignmentSubmissionRecord();
        submission.setAssignmentId(102L);
        submission.setStudentId(44L);
        submission.setContent("Initial answer");
        submission.setSubmissionDate("2026-05-20 09:00:00");
        submission.setGraded(false);
        submission.setIsLate(false);
        submission.setLatePenalty(null);
        submission.setScore(null);
        submission.setTeacherComment(null);

        AssignmentSubmissionRecord inserted = repository.insertSubmission(submission);
        inserted.setContent("Updated answer");
        inserted.setGraded(true);
        inserted.setIsLate(true);
        inserted.setLatePenalty(5);
        inserted.setScore(88);
        inserted.setTeacherComment("Good");
        repository.updateSubmission(inserted);

        assertThat(inserted.getId()).isNotNull();
        assertThat(repository.findSubmissionById(inserted.getId())).isPresent();
        assertThat(repository.findSubmissionById(inserted.getId()).orElseThrow().getScore()).isEqualTo(88);
        assertThat(repository.findSubmissionByAssignmentAndStudent(102L, 44L).orElseThrow().getContent())
                .isEqualTo("Updated answer");
        assertThat(repository.findSubmissionsByAssignmentId(102L)).extracting(AssignmentSubmissionRecord::getId)
                .contains(inserted.getId());
        assertThat(repository.findSubmissionsByStudentId(44L)).extracting(AssignmentSubmissionRecord::getId)
                .contains(inserted.getId());
        assertThat(repository.findGradedSubmissionsByStudentId(44L)).extracting(AssignmentSubmissionRecord::getId)
                .contains(inserted.getId());
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS class_students");
        jdbcTemplate.execute("DROP TABLE IF EXISTS assignment_knowledge_points");
        jdbcTemplate.execute("DROP TABLE IF EXISTS assignment_classes");
        jdbcTemplate.execute("DROP TABLE IF EXISTS assignment_submissions");
        jdbcTemplate.execute("DROP TABLE IF EXISTS assignments");
        jdbcTemplate.execute("""
                CREATE TABLE assignments (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(100) NOT NULL,
                    description TEXT,
                    course_id BIGINT NOT NULL,
                    due_date VARCHAR(30),
                    publish_date VARCHAR(30),
                    is_active BOOLEAN DEFAULT TRUE,
                    teacher_id BIGINT NOT NULL,
                    max_score INT,
                    submission_count INT DEFAULT 0,
                    graded_count INT DEFAULT 0,
                    status VARCHAR(30),
                    total_students INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE assignment_submissions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    assignment_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    content TEXT,
                    submission_date VARCHAR(30),
                    graded BOOLEAN DEFAULT FALSE,
                    is_late BOOLEAN DEFAULT FALSE,
                    late_penalty INT,
                    score INT,
                    teacher_comment TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE assignment_classes (
                    assignment_id BIGINT NOT NULL,
                    class_id BIGINT NOT NULL,
                    PRIMARY KEY (assignment_id, class_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE assignment_knowledge_points (
                    assignment_id BIGINT NOT NULL,
                    knowledge_point_id BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (assignment_id, knowledge_point_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE class_students (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    class_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL
                )
                """);
    }

    private void seedData() {
        jdbcTemplate.update("""
                INSERT INTO assignments (id, title, description, course_id, due_date, publish_date, is_active,
                                         teacher_id, max_score, submission_count, graded_count, status, total_students)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                101L, "Distributed Systems Essay", "Explain consensus", 301L,
                "2026-06-01 23:59:59", "2026-05-01 08:00:00", true,
                7L, 100, 0, 0, "pending", 2);
        jdbcTemplate.update("""
                INSERT INTO assignments (id, title, description, course_id, due_date, publish_date, is_active,
                                         teacher_id, max_score, submission_count, graded_count, status, total_students)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                102L, "Algorithm Quiz", "Greedy proof", 302L,
                "2026-07-01 23:59:59", "2026-05-02 08:00:00", true,
                7L, 50, 0, 0, "pending", 1);
        jdbcTemplate.update("INSERT INTO assignment_classes (assignment_id, class_id) VALUES (?, ?)", 101L, 501L);
        jdbcTemplate.update("INSERT INTO assignment_classes (assignment_id, class_id) VALUES (?, ?)", 102L, 503L);
        jdbcTemplate.update("INSERT INTO assignment_knowledge_points (assignment_id, knowledge_point_id) VALUES (?, ?)", 101L, 9001L);
        jdbcTemplate.update("INSERT INTO assignment_knowledge_points (assignment_id, knowledge_point_id) VALUES (?, ?)", 101L, 9003L);
        jdbcTemplate.update("INSERT INTO class_students (id, class_id, student_id) VALUES (?, ?, ?)", 701L, 501L, 42L);
        jdbcTemplate.update("INSERT INTO class_students (id, class_id, student_id) VALUES (?, ?, ?)", 702L, 501L, 43L);
        jdbcTemplate.update("INSERT INTO class_students (id, class_id, student_id) VALUES (?, ?, ?)", 703L, 503L, 44L);
        jdbcTemplate.update("""
                INSERT INTO assignment_submissions (id, assignment_id, student_id, content, submission_date, graded,
                                                    is_late, late_penalty, score, teacher_comment)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                801L, 101L, 42L, "First answer", "2026-05-10 10:00:00", true,
                false, 0, 95, "Nice");
        jdbcTemplate.update("""
                INSERT INTO assignment_submissions (id, assignment_id, student_id, content, submission_date, graded,
                                                    is_late, late_penalty, score, teacher_comment)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                802L, 101L, 43L, "Second answer", "2026-05-11 10:00:00", false,
                false, null, null, null);
    }
}
