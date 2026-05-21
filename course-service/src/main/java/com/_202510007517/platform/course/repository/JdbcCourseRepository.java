package com._202510007517.platform.course.repository;

import com._202510007517.platform.course.domain.CourseRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcCourseRepository implements CourseRepository {

    private static final RowMapper<CourseRecord> COURSE_ROW_MAPPER = new CourseRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCourseRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CourseRecord> findByTeacherIdWithSearch(Long teacherId, String name, String courseCode, String category, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource("teacherId", teacherId);
        StringBuilder sql = new StringBuilder("""
                SELECT id, course_name, course_code, description, credit, course_category,
                       total_hours, teacher_id, course_director, assessment_method,
                       course_status, semester, start_date, end_date, max_students
                FROM courses
                WHERE teacher_id = :teacherId
                """);
        appendLike(sql, params, "course_name", "name", name);
        appendEquals(sql, params, "course_code", "courseCode", courseCode);
        appendEquals(sql, params, "course_category", "category", category);
        appendEquals(sql, params, "course_status", "status", status);
        sql.append(" ORDER BY id DESC");
        return jdbcTemplate.query(sql.toString(), params, COURSE_ROW_MAPPER);
    }

    @Override
    public List<CourseRecord> findByStudentIdWithSearch(Long studentId, String searchQuery, String category, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource("studentId", studentId);
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT c.id, c.course_name, c.course_code, c.description, c.credit, c.course_category,
                       c.total_hours, c.teacher_id, c.course_director, c.assessment_method,
                       c.course_status, c.semester, c.start_date, c.end_date, c.max_students
                FROM class_students cs
                JOIN course_classes cc ON cc.id = cs.class_id
                LEFT JOIN class_courses ccl ON ccl.class_id = cc.id
                JOIN courses c ON c.id = COALESCE(ccl.course_id, cc.course_id)
                WHERE cs.student_id = :studentId
                """);
        appendLike(sql, params, "c.course_name", "searchQuery", searchQuery);
        appendEquals(sql, params, "c.course_category", "category", category);
        appendEquals(sql, params, "c.course_status", "status", status);
        sql.append(" ORDER BY c.id DESC");
        return jdbcTemplate.query(sql.toString(), params, COURSE_ROW_MAPPER);
    }

    @Override
    public List<CourseRecord> findAllCourses() {
        return jdbcTemplate.query("""
                SELECT id, course_name, course_code, description, credit, course_category,
                       total_hours, teacher_id, course_director, assessment_method,
                       course_status, semester, start_date, end_date, max_students
                FROM courses
                ORDER BY id DESC
                """, COURSE_ROW_MAPPER);
    }

    @Override
    public Optional<CourseRecord> findById(Long id) {
        List<CourseRecord> rows = jdbcTemplate.query("""
                SELECT id, course_name, course_code, description, credit, course_category,
                       total_hours, teacher_id, course_director, assessment_method,
                       course_status, semester, start_date, end_date, max_students
                FROM courses
                WHERE id = :id
                """, new MapSqlParameterSource("id", id), COURSE_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    @Override
    public CourseRecord insert(CourseRecord course) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO courses
                    (course_name, course_code, description, credit, course_category, total_hours,
                     teacher_id, course_director, assessment_method, course_status, semester,
                     start_date, end_date, max_students)
                VALUES
                    (:courseName, :courseCode, :description, :credit, :courseCategory, :totalHours,
                     :teacherId, :courseDirector, :assessmentMethod, :courseStatus, :semester,
                     :startDate, :endDate, :maxStudents)
                """, params(course), keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            course.setId(keyHolder.getKey().longValue());
        }
        return course;
    }

    @Override
    public void update(CourseRecord course) {
        jdbcTemplate.update("""
                UPDATE courses
                SET course_name = :courseName,
                    course_code = :courseCode,
                    description = :description,
                    credit = :credit,
                    course_category = :courseCategory,
                    total_hours = :totalHours,
                    teacher_id = :teacherId,
                    course_director = :courseDirector,
                    assessment_method = :assessmentMethod,
                    course_status = :courseStatus,
                    semester = :semester,
                    start_date = :startDate,
                    end_date = :endDate,
                    max_students = :maxStudents
                WHERE id = :id
                """, params(course));
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM class_courses WHERE course_id = :id", new MapSqlParameterSource("id", id));
        jdbcTemplate.update("UPDATE course_classes SET course_id = NULL WHERE course_id = :id", new MapSqlParameterSource("id", id));
        jdbcTemplate.update("DELETE FROM courses WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    @Override
    public Map<Long, Integer> countStudentsByCourseIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("courseIds", courseIds);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT c.id AS courseId,
                       COUNT(DISTINCT cs.student_id) AS studentCount
                FROM courses c
                LEFT JOIN course_classes cc_direct ON cc_direct.course_id = c.id
                LEFT JOIN class_courses cc_link ON cc_link.course_id = c.id
                LEFT JOIN class_students cs ON cs.class_id = cc_direct.id OR cs.class_id = cc_link.class_id
                WHERE c.id IN (:courseIds)
                GROUP BY c.id
                """, params);
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("courseId")).longValue(), ((Number) row.get("studentCount")).intValue());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> findStudentsByCourseId(Long courseId) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT cs.student_id AS id, cc.class_name AS className
                FROM class_students cs
                JOIN course_classes cc ON cs.class_id = cc.id
                LEFT JOIN class_courses ccl ON ccl.class_id = cc.id
                WHERE cc.course_id = :courseId OR ccl.course_id = :courseId
                ORDER BY cs.student_id
                """, new MapSqlParameterSource("courseId", courseId));
    }

    @Override
    public List<Long> findClassIdsByStudentId(Long studentId) {
        return jdbcTemplate.query("""
                SELECT DISTINCT class_id
                FROM class_students
                WHERE student_id = :studentId
                ORDER BY class_id
                """, new MapSqlParameterSource("studentId", studentId),
                (rs, rowNum) -> rs.getLong("class_id"));
    }

    @Override
    public List<Long> findStudentIdsByTeacherId(Long teacherId) {
        return jdbcTemplate.query("""
                SELECT DISTINCT cs.student_id
                FROM class_students cs
                JOIN course_classes cc ON cc.id = cs.class_id
                LEFT JOIN class_courses ccl ON ccl.class_id = cc.id
                LEFT JOIN courses direct_course ON direct_course.id = cc.course_id
                LEFT JOIN courses linked_course ON linked_course.id = ccl.course_id
                WHERE cc.teacher_id = :teacherId
                   OR ccl.teacher_id = :teacherId
                   OR direct_course.teacher_id = :teacherId
                   OR linked_course.teacher_id = :teacherId
                ORDER BY cs.student_id
                """, new MapSqlParameterSource("teacherId", teacherId),
                (rs, rowNum) -> rs.getLong("student_id"));
    }

    @Override
    public List<Map<String, Object>> findClassesByTeacherId(Long teacherId, String className, String grade, String majorName, Long majorId, Long courseId) {
        MapSqlParameterSource params = new MapSqlParameterSource("teacherId", teacherId);
        StringBuilder sql = new StringBuilder("""
                SELECT cc.id AS id,
                       cc.class_name AS className,
                       cc.year AS year,
                       cc.capacity AS capacity,
                       COUNT(DISTINCT cs.student_id) AS studentCount,
                       cc.teacher_id AS teacherId,
                       NULL AS teacherName,
                       cc.major_id AS majorId,
                       m.major_name AS majorName,
                       c.id AS courseId,
                       c.course_name AS courseName,
                       COALESCE(ccl.class_time, cc.class_time) AS classTime,
                       COALESCE(ccl.class_location, cc.class_location) AS classLocation
                FROM course_classes cc
                LEFT JOIN class_courses ccl ON ccl.class_id = cc.id
                LEFT JOIN courses c ON c.id = COALESCE(ccl.course_id, cc.course_id)
                LEFT JOIN majors m ON m.id = cc.major_id
                LEFT JOIN class_students cs ON cs.class_id = cc.id
                WHERE (cc.teacher_id = :teacherId OR ccl.teacher_id = :teacherId OR c.teacher_id = :teacherId)
                """);
        appendLike(sql, params, "cc.class_name", "className", className);
        appendEquals(sql, params, "cc.year", "grade", grade);
        appendLike(sql, params, "m.major_name", "majorName", majorName);
        appendEquals(sql, params, "cc.major_id", "majorId", majorId);
        appendEquals(sql, params, "c.id", "courseId", courseId);
        sql.append("""
                
                GROUP BY cc.id, cc.class_name, cc.year, cc.capacity, cc.teacher_id, cc.major_id,
                         m.major_name, c.id, c.course_name, ccl.class_time, cc.class_time,
                         ccl.class_location, cc.class_location
                ORDER BY cc.id DESC, c.id DESC
                """);
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    @Override
    public Map<String, Object> findClassById(Long classId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT cc.id AS id,
                       cc.class_name AS className,
                       cc.year AS year,
                       cc.capacity AS capacity,
                       cc.course_id AS courseId,
                       cc.teacher_id AS teacherId,
                       cc.major_id AS majorId,
                       m.major_name AS majorName,
                       cc.class_time AS classTime,
                       cc.class_location AS classLocation,
                       COUNT(DISTINCT cs.student_id) AS studentCount
                FROM course_classes cc
                LEFT JOIN majors m ON m.id = cc.major_id
                LEFT JOIN class_students cs ON cs.class_id = cc.id
                WHERE cc.id = :classId
                GROUP BY cc.id, cc.class_name, cc.year, cc.capacity, cc.course_id, cc.teacher_id,
                         cc.major_id, m.major_name, cc.class_time, cc.class_location
                """, new MapSqlParameterSource("classId", classId));
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    @Override
    public List<Map<String, Object>> findStudentsByClassId(Long classId) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT student_id AS id
                FROM class_students
                WHERE class_id = :classId
                ORDER BY student_id
                """, new MapSqlParameterSource("classId", classId));
    }

    @Override
    public Long insertClass(Map<String, Object> classData) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO course_classes
                    (class_name, year, capacity, course_id, teacher_id, major_id, class_time, class_location)
                VALUES
                    (:className, :year, :capacity, :courseId, :teacherId, :majorId, :classTime, :classLocation)
                """, new MapSqlParameterSource(classData), keyHolder, new String[]{"id"});
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    @Override
    public void updateClass(Map<String, Object> classData) {
        jdbcTemplate.update("""
                UPDATE course_classes
                SET class_name = :className,
                    year = :year,
                    capacity = :capacity,
                    course_id = :courseId,
                    teacher_id = :teacherId,
                    major_id = :majorId,
                    class_time = :classTime,
                    class_location = :classLocation
                WHERE id = :id
                """, new MapSqlParameterSource(classData));
    }

    @Override
    public void deleteClass(Long classId) {
        MapSqlParameterSource params = new MapSqlParameterSource("classId", classId);
        jdbcTemplate.update("DELETE FROM class_students WHERE class_id = :classId", params);
        jdbcTemplate.update("DELETE FROM class_courses WHERE class_id = :classId", params);
        jdbcTemplate.update("DELETE FROM course_classes WHERE id = :classId", params);
    }

    @Override
    public List<Map<String, Object>> findClassAssignments(Long teacherId, Long courseId, Long classId) {
        MapSqlParameterSource params = new MapSqlParameterSource("teacherId", teacherId);
        StringBuilder sql = new StringBuilder("""
                SELECT ccl.id AS assignmentId,
                       cc.id AS classId,
                       c.id AS courseId,
                       c.course_name AS courseName,
                       cc.class_name AS className,
                       NULL AS teacherName,
                       c.semester AS semester,
                       ccl.class_time AS weeklyHours,
                       ccl.class_location AS classLocation
                FROM class_courses ccl
                JOIN course_classes cc ON cc.id = ccl.class_id
                JOIN courses c ON c.id = ccl.course_id
                WHERE (cc.teacher_id = :teacherId OR ccl.teacher_id = :teacherId OR c.teacher_id = :teacherId)
                """);
        appendEquals(sql, params, "c.id", "courseId", courseId);
        appendEquals(sql, params, "cc.id", "classId", classId);
        sql.append(" ORDER BY ccl.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    @Override
    public Long insertClassCourse(Map<String, Object> assignData) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO class_courses (class_id, course_id, teacher_id, class_time, class_location)
                VALUES (:classId, :courseId, :teacherId, :classTime, :classLocation)
                """, new MapSqlParameterSource(assignData), keyHolder, new String[]{"id"});
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    @Override
    public void deleteClassCourse(Long assignmentId) {
        jdbcTemplate.update("DELETE FROM class_courses WHERE id = :assignmentId",
                new MapSqlParameterSource("assignmentId", assignmentId));
    }

    @Override
    public void deleteClassCourse(Long classId, Long courseId) {
        jdbcTemplate.update("DELETE FROM class_courses WHERE class_id = :classId AND course_id = :courseId",
                new MapSqlParameterSource("classId", classId).addValue("courseId", courseId));
    }

    @Override
    public boolean teacherOwnsCourse(Long teacherId, Long courseId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM courses
                WHERE id = :courseId AND teacher_id = :teacherId
                """, new MapSqlParameterSource("teacherId", teacherId).addValue("courseId", courseId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean teacherCanAccessClass(Long teacherId, Long classId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT cc.id)
                FROM course_classes cc
                LEFT JOIN courses direct_course ON direct_course.id = cc.course_id
                LEFT JOIN class_courses ccl ON ccl.class_id = cc.id
                LEFT JOIN courses linked_course ON linked_course.id = ccl.course_id
                WHERE cc.id = :classId
                  AND (cc.teacher_id = :teacherId
                       OR ccl.teacher_id = :teacherId
                       OR direct_course.teacher_id = :teacherId
                       OR linked_course.teacher_id = :teacherId)
                """, new MapSqlParameterSource("teacherId", teacherId).addValue("classId", classId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean classCourseExists(Long classId, Long courseId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM class_courses
                WHERE class_id = :classId AND course_id = :courseId
                """, new MapSqlParameterSource("classId", classId).addValue("courseId", courseId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean classNameExists(String className, Long excludedClassId) {
        MapSqlParameterSource params = new MapSqlParameterSource("className", className);
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM course_classes
                WHERE class_name = :className
                """);
        if (excludedClassId != null) {
            sql.append(" AND id != :excludedClassId");
            params.addValue("excludedClassId", excludedClassId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public void replaceStudentClass(Long studentId, Long classId) {
        MapSqlParameterSource params = new MapSqlParameterSource("studentId", studentId)
                .addValue("classId", classId);
        jdbcTemplate.update("DELETE FROM class_students WHERE student_id = :studentId", params);
        jdbcTemplate.update("""
                INSERT IGNORE INTO class_students (class_id, student_id)
                VALUES (:classId, :studentId)
                """, params);
    }

    @Override
    public List<Map<String, Object>> findMajors() {
        return jdbcTemplate.queryForList("""
                SELECT id AS id, major_name AS majorName
                FROM majors
                ORDER BY id
                """, Map.of());
    }

    private static void appendEquals(StringBuilder sql, MapSqlParameterSource params, String column, String param, String value) {
        if (value != null && !value.isBlank()) {
            sql.append(System.lineSeparator()).append(" AND ").append(column).append(" = :").append(param);
            params.addValue(param, value);
        }
    }

    private static void appendEquals(StringBuilder sql, MapSqlParameterSource params, String column, String param, Long value) {
        if (value != null) {
            sql.append(System.lineSeparator()).append(" AND ").append(column).append(" = :").append(param);
            params.addValue(param, value);
        }
    }

    private static void appendLike(StringBuilder sql, MapSqlParameterSource params, String column, String param, String value) {
        if (value != null && !value.isBlank()) {
            sql.append(System.lineSeparator()).append(" AND ").append(column).append(" LIKE :").append(param);
            params.addValue(param, "%" + value + "%");
        }
    }

    private static MapSqlParameterSource params(CourseRecord course) {
        return new MapSqlParameterSource()
                .addValue("id", course.getId())
                .addValue("courseName", course.getCourseName())
                .addValue("courseCode", course.getCourseCode())
                .addValue("description", course.getDescription())
                .addValue("credit", course.getCredit())
                .addValue("courseCategory", course.getCourseCategory())
                .addValue("totalHours", course.getTotalHours())
                .addValue("teacherId", course.getTeacherId())
                .addValue("courseDirector", course.getCourseDirector())
                .addValue("assessmentMethod", course.getAssessmentMethod())
                .addValue("courseStatus", course.getCourseStatus())
                .addValue("semester", course.getSemester())
                .addValue("startDate", course.getStartDate())
                .addValue("endDate", course.getEndDate())
                .addValue("maxStudents", course.getMaxStudents());
    }

    private static class CourseRowMapper implements RowMapper<CourseRecord> {
        @Override
        public CourseRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            CourseRecord course = new CourseRecord();
            course.setId(rs.getLong("id"));
            course.setCourseName(rs.getString("course_name"));
            course.setCourseCode(rs.getString("course_code"));
            course.setDescription(rs.getString("description"));
            course.setCredit((Integer) rs.getObject("credit"));
            course.setCourseCategory(rs.getString("course_category"));
            course.setTotalHours((Integer) rs.getObject("total_hours"));
            course.setTeacherId((Long) rs.getObject("teacher_id"));
            course.setCourseDirector((Long) rs.getObject("course_director"));
            course.setAssessmentMethod(rs.getString("assessment_method"));
            course.setCourseStatus(rs.getString("course_status"));
            course.setSemester(rs.getString("semester"));
            course.setStartDate(rs.getString("start_date"));
            course.setEndDate(rs.getString("end_date"));
            course.setMaxStudents((Integer) rs.getObject("max_students"));
            return course;
        }
    }
}
