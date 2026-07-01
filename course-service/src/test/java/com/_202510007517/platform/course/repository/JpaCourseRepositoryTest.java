package com._202510007517.platform.course.repository;

import com._202510007517.platform.course.CourseServiceApplication;
import com._202510007517.platform.course.domain.CourseRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CourseServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaCourseRepositoryTest {

    private static final String DATABASE_NAME = "course-jpa-" + UUID.randomUUID();

    @Autowired
    private CourseRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=year;DB_CLOSE_DELAY=-1");
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
    void findsTeacherAndStudentCoursesThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaCourseRepository.class);

        List<CourseRecord> teacherCourses = repository.findByTeacherIdWithSearch(7L, "Distributed", "DS101", "专业课", "进行中");
        List<CourseRecord> studentCourses = repository.findByStudentIdWithSearch(42L, "Distributed", "专业课", "进行中");
        Optional<CourseRecord> loaded = repository.findById(101L);

        assertThat(teacherCourses).extracting(CourseRecord::getId).containsExactly(101L);
        assertThat(studentCourses).extracting(CourseRecord::getId).containsExactly(101L);
        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getCourseName()).isEqualTo("Distributed Systems");
        assertThat(repository.findAllCourses()).extracting(CourseRecord::getId).containsExactly(102L, 101L);
        assertThat(repository.countStudentsByCourseIds(List.of(101L, 102L)))
                .containsEntry(101L, 2)
                .containsEntry(102L, 1);
    }

    @Test
    void mutatesCoursesAndAssignmentsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaCourseRepository.class);

        CourseRecord course = new CourseRecord();
        course.setCourseName("Operating Systems");
        course.setCourseCode("OS201");
        course.setDescription("Kernel and process management");
        course.setCredit(4);
        course.setCourseCategory("专业课");
        course.setTotalHours(64);
        course.setTeacherId(7L);
        course.setCourseDirector(9L);
        course.setAssessmentMethod("考试");
        course.setCourseStatus("进行中");
        course.setSemester("2026秋");
        course.setStartDate("2026-09-01");
        course.setEndDate("2026-12-31");
        course.setMaxStudents(80);

        CourseRecord inserted = repository.insert(course);
        inserted.setCourseName("Operating Systems Updated");
        inserted.setCourseStatus("已结束");
        repository.update(inserted);

        Long classId = repository.insertClass(Map.of(
                "className", "软件 2302",
                "year", "2023",
                "capacity", 45,
                "courseId", inserted.getId(),
                "teacherId", 7L,
                "majorId", 2L,
                "classTime", "周四 1-2",
                "classLocation", "B201"
        ));

        Long assignmentId = repository.insertClassCourse(Map.of(
                "classId", classId,
                "courseId", inserted.getId(),
                "teacherId", 7L,
                "classTime", "周四 1-2",
                "classLocation", "B201"
        ));

        assertThat(inserted.getId()).isNotNull();
        assertThat(repository.findById(inserted.getId())).isPresent();
        assertThat(repository.findById(inserted.getId()).orElseThrow().getCourseName()).isEqualTo("Operating Systems Updated");
        assertThat(classId).isNotNull();
        assertThat(assignmentId).isNotNull();
        assertThat(repository.classCourseExists(classId, inserted.getId())).isTrue();
        assertThat(repository.teacherOwnsCourse(7L, inserted.getId())).isTrue();

        repository.deleteClassCourse(assignmentId);
        assertThat(repository.classCourseExists(classId, inserted.getId())).isFalse();

        repository.delete(inserted.getId());
        assertThat(repository.findById(inserted.getId())).isEmpty();
    }

    @Test
    void resolvesClassAndStudentAccessPatternsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaCourseRepository.class);

        List<Map<String, Object>> classes = repository.findClassesByTeacherId(7L, "软件 2301", "2023", "软件工程", 2L, 101L);
        Map<String, Object> classRow = repository.findClassById(501L);
        List<Map<String, Object>> classStudents = repository.findStudentsByClassId(501L);
        List<Map<String, Object>> courseStudents = repository.findStudentsByCourseId(101L);
        List<Map<String, Object>> assignments = repository.findClassAssignments(7L, 101L, 501L);

        assertThat(classes).hasSize(1);
        assertThat(classRow).containsEntry("className", "软件 2301");
        assertThat(classStudents).extracting(row -> ((Number) row.get("id")).longValue()).containsExactly(42L, 43L);
        assertThat(courseStudents).extracting(row -> ((Number) row.get("id")).longValue()).containsExactly(42L, 43L);
        assertThat(assignments).hasSize(1);
        assertThat(repository.findClassIdsByStudentId(42L)).containsExactly(501L);
        assertThat(repository.findStudentIdsByTeacherId(7L)).containsExactly(42L, 43L);
        assertThat(repository.teacherCanAccessClass(7L, 501L)).isTrue();
        assertThat(repository.teacherCanAccessClass(8L, 501L)).isFalse();
        assertThat(repository.classNameExists("软件 2301", null)).isTrue();
        assertThat(repository.classNameExists("软件 2301", 501L)).isFalse();
        assertThat(repository.findMajors()).extracting(row -> row.get("majorName")).containsExactly("软件工程");
    }

    @Test
    void replacesStudentClassThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaCourseRepository.class);
        jdbcTemplate.update("""
                INSERT INTO course_classes (id, class_name, year, capacity, course_id, teacher_id, major_id, class_time, class_location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 502L, "软件 2302", "2023", 45, 102L, 7L, 2L, "周四 1-2", "B201");

        repository.replaceStudentClass(42L, 502L);

        assertThat(repository.findClassIdsByStudentId(42L)).containsExactly(502L);
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS class_students");
        jdbcTemplate.execute("DROP TABLE IF EXISTS class_courses");
        jdbcTemplate.execute("DROP TABLE IF EXISTS course_classes");
        jdbcTemplate.execute("DROP TABLE IF EXISTS courses");
        jdbcTemplate.execute("DROP TABLE IF EXISTS majors");
        jdbcTemplate.execute("""
                CREATE TABLE majors (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    major_name VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE courses (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    course_name VARCHAR(50) NOT NULL,
                    course_code VARCHAR(20) NOT NULL,
                    description VARCHAR(200),
                    credit INT NOT NULL,
                    course_category VARCHAR(50),
                    total_hours INT NOT NULL,
                    teacher_id BIGINT NOT NULL,
                    course_director BIGINT,
                    assessment_method VARCHAR(50),
                    course_status VARCHAR(30),
                    semester VARCHAR(50),
                    start_date VARCHAR(20),
                    end_date VARCHAR(20),
                    max_students INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_courses_code UNIQUE (course_code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE course_classes (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    class_name VARCHAR(100) NOT NULL,
                    year VARCHAR(20),
                    capacity INT,
                    course_id BIGINT NULL,
                    teacher_id BIGINT,
                    major_id BIGINT,
                    class_time VARCHAR(100),
                    class_location VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE class_courses (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    class_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    teacher_id BIGINT,
                    class_time VARCHAR(100),
                    class_location VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_class_courses UNIQUE (class_id, course_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE class_students (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    class_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_class_students UNIQUE (class_id, student_id)
                )
                """);
    }

    private void seedData() {
        jdbcTemplate.update("INSERT INTO majors (id, major_name) VALUES (?, ?)", 2L, "软件工程");
        jdbcTemplate.update("""
                INSERT INTO courses (id, course_name, course_code, description, credit, course_category, total_hours,
                                     teacher_id, course_director, assessment_method, course_status, semester,
                                     start_date, end_date, max_students)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                101L, "Distributed Systems", "DS101", "Distributed systems fundamentals", 3, "专业课", 48,
                7L, 9L, "考试", "进行中", "2026秋", "2026-09-01", "2026-12-31", 60);
        jdbcTemplate.update("""
                INSERT INTO courses (id, course_name, course_code, description, credit, course_category, total_hours,
                                     teacher_id, course_director, assessment_method, course_status, semester,
                                     start_date, end_date, max_students)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                102L, "Algorithms", "ALG102", "Algorithm design", 4, "专业课", 64,
                8L, 10L, "考试", "已结束", "2026春", "2026-03-01", "2026-06-30", 80);
        jdbcTemplate.update("""
                INSERT INTO course_classes (id, class_name, year, capacity, course_id, teacher_id, major_id, class_time, class_location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 501L, "软件 2301", "2023", 40, 101L, 7L, 2L, "周一 1-2", "A101");
        jdbcTemplate.update("""
                INSERT INTO course_classes (id, class_name, year, capacity, course_id, teacher_id, major_id, class_time, class_location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 503L, "软件 2303", "2023", 35, 102L, 8L, 2L, "周三 3-4", "C301");
        jdbcTemplate.update("""
                INSERT INTO class_courses (id, class_id, course_id, teacher_id, class_time, class_location)
                VALUES (?, ?, ?, ?, ?, ?)
                """, 601L, 501L, 101L, 7L, "周一 1-2", "A101");
        jdbcTemplate.update("INSERT INTO class_students (id, class_id, student_id) VALUES (?, ?, ?)", 701L, 501L, 42L);
        jdbcTemplate.update("INSERT INTO class_students (id, class_id, student_id) VALUES (?, ?, ?)", 702L, 501L, 43L);
        jdbcTemplate.update("INSERT INTO class_students (id, class_id, student_id) VALUES (?, ?, ?)", 703L, 503L, 44L);
    }
}
