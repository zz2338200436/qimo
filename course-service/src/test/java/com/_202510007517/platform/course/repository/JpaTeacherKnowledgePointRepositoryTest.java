package com._202510007517.platform.course.repository;

import com._202510007517.platform.course.CourseServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CourseServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaTeacherKnowledgePointRepositoryTest {

    private static final String DATABASE_NAME = "course-kp-jpa-" + UUID.randomUUID();

    @Autowired
    private TeacherKnowledgePointRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("eureka.client.register-with-eureka", () -> "false");
        registry.add("eureka.client.fetch-registry", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
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
    }

    @Test
    void managesTeacherKnowledgePointsThroughJpaRepository() {
        assertThat(AopUtils.getTargetClass(repository).getSimpleName()).isEqualTo("JpaTeacherKnowledgePointRepository");

        seedCourse(101L, 7L, "Java Web");
        seedCourse(102L, 8L, "Python");
        seedKnowledgePoint(1001L, "集合框架", "List/Map", "中等", 1, 101L);
        seedKnowledgePoint(1002L, "JPA", "实体映射", "较难", 2, 101L);
        seedKnowledgePoint(1003L, "装饰器", "Python 特性", "中等", 1, 102L);

        assertThat(repository.findByTeacherId(7L))
                .extracting(row -> row.get("id"))
                .containsExactly(1001L, 1002L);
        assertThat(repository.findByTeacherIdAndCourseId(7L, 101L))
                .extracting(row -> row.get("pointName"))
                .containsExactly("集合框架", "JPA");
        assertThat(repository.findByTeacherIdAndKnowledgePointId(7L, 1001L))
                .hasValueSatisfying(row -> assertThat(row)
                        .containsEntry("courseName", "Java Web"));
        assertThat(repository.findByTeacherIdAndKnowledgePointId(7L, 1003L)).isEmpty();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pointName", "事务管理");
        payload.put("description", "传播行为");
        payload.put("difficulty", "较难");
        payload.put("orderIndex", 3);
        payload.put("courseId", 101L);
        Long createdId = repository.insert(payload);

        assertThat(createdId).isNotNull();
        assertThat(repository.findByTeacherIdAndKnowledgePointId(7L, createdId))
                .hasValueSatisfying(row -> assertThat(row)
                        .containsEntry("pointName", "事务管理")
                        .containsEntry("courseName", "Java Web"));

        payload.put("id", createdId);
        payload.put("pointName", "Spring 事务");
        payload.put("description", "声明式事务");
        payload.put("difficulty", "中等");
        payload.put("orderIndex", 4);
        repository.update(payload);

        assertThat(repository.findByTeacherIdAndKnowledgePointId(7L, createdId))
                .hasValueSatisfying(row -> assertThat(row)
                        .containsEntry("pointName", "Spring 事务")
                        .containsEntry("description", "声明式事务")
                        .containsEntry("orderIndex", 4));

        repository.delete(createdId);
        assertThat(repository.findByTeacherIdAndKnowledgePointId(7L, createdId)).isEmpty();
    }

    private void seedCourse(Long id, Long teacherId, String courseName) {
        jdbcTemplate.update("""
                INSERT INTO courses (
                    id, course_name, course_code, description, credit, course_category, total_hours,
                    teacher_id, course_status, semester
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                courseName,
                "C-" + id,
                courseName + " 描述",
                3,
                "专业课",
                48,
                teacherId,
                "ACTIVE",
                "2025-2026-1");
    }

    private void seedKnowledgePoint(
            Long id,
            String pointName,
            String description,
            String difficulty,
            Integer orderIndex,
            Long courseId) {
        jdbcTemplate.update("""
                INSERT INTO teacher_knowledge_points (
                    id, point_name, description, difficulty, order_index, course_id
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id,
                pointName,
                description,
                difficulty,
                orderIndex,
                courseId);
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS teacher_knowledge_points");
        jdbcTemplate.execute("DROP TABLE IF EXISTS courses");
        jdbcTemplate.execute("""
                CREATE TABLE courses (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    course_name VARCHAR(50) NOT NULL,
                    course_code VARCHAR(20) NOT NULL,
                    description VARCHAR(200) NULL,
                    credit INT NOT NULL,
                    course_category VARCHAR(50) NULL,
                    total_hours INT NOT NULL,
                    teacher_id BIGINT NOT NULL,
                    course_director BIGINT NULL,
                    assessment_method VARCHAR(50) NULL,
                    course_status VARCHAR(30) NULL,
                    semester VARCHAR(50) NULL,
                    start_date VARCHAR(20) NULL,
                    end_date VARCHAR(20) NULL,
                    max_students INT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE teacher_knowledge_points (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    point_name VARCHAR(255) NOT NULL,
                    description TEXT NULL,
                    difficulty VARCHAR(50) NULL,
                    order_index INT DEFAULT 0,
                    course_id BIGINT NOT NULL,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                )
                """);
    }
}
