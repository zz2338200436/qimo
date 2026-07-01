package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.Course;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface CourseMapper {
    // 动态 WHERE → 见 CourseMapper.xml
    List<Course> findByTeacherIdWithSearch(@Param("teacherId") Long teacherId,
                                          @Param("name") String name,
                                          @Param("courseCode") String courseCode,
                                          @Param("category") String category,
                                          @Param("status") String status);

    @Select("SELECT id, course_name, course_code, description, credit, course_category, total_hours, teacher_id, course_director, assessment_method, course_status, semester, start_date, end_date, max_students FROM courses WHERE teacher_id = #{teacherId}")
    List<Course> findByTeacherId(Long teacherId);

    @Select("SELECT id, course_name, course_code, description, credit, course_category, total_hours, teacher_id, course_director, assessment_method, course_status, semester, start_date, end_date, max_students FROM courses")
    List<Course> getAllCourses();

    @Insert("INSERT INTO courses (course_name, course_code, description, credit, course_category, total_hours, teacher_id, course_director, assessment_method, course_status, semester, start_date, end_date, max_students) VALUES (#{courseName}, #{courseCode}, #{description}, #{credit}, #{courseCategory}, #{totalHours}, #{teacherId}, #{courseDirector}, #{assessmentMethod}, #{courseStatus}, #{semester}, #{startDate}, #{endDate}, #{maxStudents})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Course course);

    @Update("UPDATE courses SET course_name = #{courseName}, course_code = #{courseCode}, description = #{description}, credit = #{credit}, course_category = #{courseCategory}, total_hours = #{totalHours}, teacher_id = #{teacherId}, course_director = #{courseDirector}, assessment_method = #{assessmentMethod}, course_status = #{courseStatus}, semester = #{semester}, start_date = #{startDate}, end_date = #{endDate}, max_students = #{maxStudents} WHERE id = #{id}")
    void update(Course course);

    @Select("SELECT id, course_name, course_code, description, credit, course_category, total_hours, teacher_id, course_director, assessment_method, course_status, semester, start_date, end_date, max_students FROM courses WHERE id = #{id}")
    Course findById(Long id);

    // UNION 子查询（CASE/WITH/UNION 规则）→ 见 CourseMapper.xml
    Integer getStudentCountByCourseId(Long courseId);

    // foreach + UNION 子查询 → 见 CourseMapper.xml
    List<Map<String, Object>> batchGetStudentCountByCourseIds(@Param("courseIds") List<Long> courseIds);

    // UNION ALL 子查询 → 见 CourseMapper.xml
    Double getCourseAverageScore(Long courseId);

    // foreach + UNION ALL 子查询 → 见 CourseMapper.xml
    List<Map<String, Object>> batchGetCourseAverageScoresByCourseIds(@Param("courseIds") List<Long> courseIds);

    // UNION ALL 子查询 → 见 CourseMapper.xml
    List<Double> getCourseScores(Long courseId);

    @Select("SELECT AVG(score) FROM assignment_submissions WHERE assignment_id IN (SELECT id FROM assignments WHERE course_id = #{courseId}) AND graded = true AND score IS NOT NULL")
    Double getAverageScoreByCourseId(Long courseId);

    @Delete("DELETE FROM courses WHERE id = #{id}")
    void delete(Long id);

    // 删除课程关联数据的方法
    @Delete("DELETE FROM assignment_knowledge_points WHERE knowledge_point_id IN (SELECT id FROM knowledge_points WHERE course_id = #{courseId})")
    void deleteAssignmentKnowledgePointsByCourseId(Long courseId);

    @Delete("DELETE FROM exam_knowledge_points WHERE knowledge_point_id IN (SELECT id FROM knowledge_points WHERE course_id = #{courseId})")
    void deleteExamKnowledgePointsByCourseId(Long courseId);

    @Delete("DELETE FROM knowledge_mastery WHERE knowledge_point_id IN (SELECT id FROM knowledge_points WHERE course_id = #{courseId})")
    void deleteKnowledgeMasteryByCourseId(Long courseId);

    @Delete("DELETE FROM questions WHERE knowledge_point_id IN (SELECT id FROM knowledge_points WHERE course_id = #{courseId})")
    void deleteQuestionsByCourseId(Long courseId);

    @Delete("DELETE FROM knowledge_points WHERE course_id = #{courseId}")
    void deleteKnowledgePointsByCourseId(Long courseId);

    @Delete("DELETE FROM early_warnings WHERE course_id = #{courseId}")
    void deleteEarlyWarningsByCourseId(Long courseId);

    @Delete("DELETE FROM class_courses WHERE course_id = #{courseId}")
    void deleteClassCoursesByCourseId(Long courseId);

    @Delete("DELETE FROM course_classes WHERE course_id = #{courseId}")
    void deleteCourseClassesByCourseId(Long courseId);

    @Delete("DELETE FROM assignment_submissions WHERE assignment_id IN (SELECT id FROM assignments WHERE course_id = #{courseId})")
    void deleteAssignmentSubmissionsByCourseId(Long courseId);

    @Delete("DELETE FROM assignment_classes WHERE assignment_id IN (SELECT id FROM assignments WHERE course_id = #{courseId})")
    void deleteAssignmentClassesByCourseId(Long courseId);

    @Delete("DELETE FROM assignments WHERE course_id = #{courseId}")
    void deleteAssignmentsByCourseId(Long courseId);

    @Delete("DELETE FROM exam_submissions WHERE exam_id IN (SELECT id FROM exams WHERE course_id = #{courseId})")
    void deleteExamSubmissionsByCourseId(Long courseId);

    @Delete("DELETE FROM exam_classes WHERE exam_id IN (SELECT id FROM exams WHERE course_id = #{courseId})")
    void deleteExamClassesByCourseId(Long courseId);

    @Delete("DELETE FROM exams WHERE course_id = #{courseId}")
    void deleteExamsByCourseId(Long courseId);

    // UNION 子查询 → 见 CourseMapper.xml
    List<Long> getStudentIdsByCourseId(Long courseId);

    @Select("SELECT * FROM audit_logs ORDER BY operation_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getRecentAuditLogs(@Param("limit") int limit);

    // 多表 JOIN + 动态 WHERE → 见 CourseMapper.xml
    List<Map<String, Object>> getClassesByTeacherId(@Param("teacherId") Long teacherId,
                                                    @Param("className") String className,
                                                    @Param("grade") String grade,
                                                    @Param("majorName") String majorName,
                                                    @Param("majorId") Long majorId,
                                                    @Param("teacherName") String teacherName,
                                                    @Param("courseId") Long courseId);

    // UNION 子查询 → 见 CourseMapper.xml
    List<Long> getStudentIdsByCourseIdAndClassId(@Param("courseId") Long courseId, @Param("classId") Long classId);

    // UNION 子查询 → 见 CourseMapper.xml
    Integer getStudentCountByCourseIdAndClassId(@Param("courseId") Long courseId, @Param("classId") Long classId);

    // 多表 JOIN + UNION ALL → 见 CourseMapper.xml
    Double getCourseAverageScoreByClassId(@Param("courseId") Long courseId, @Param("classId") Long classId);

    // 多表 JOIN → 见 CourseMapper.xml
    List<Map<String, Object>> getStudentsByCourseId(Long courseId);

    // UNION 子查询 → 见 CourseMapper.xml
    List<Long> getStudentIdsByClassTeacherId(Long teacherId);

    // 班级管理相关方法
    void createClass(Map<String, Object> classData);
    void updateClass(Map<String, Object> classData);
    void deleteClass(Long classId);

    // 检查班级名称是否存在
    @Select("SELECT COUNT(*) FROM course_classes WHERE class_name = #{className} AND (id != #{classId} OR #{classId} IS NULL)")
    Integer checkClassNameExists(@Param("className") String className, @Param("classId") Long classId);

    // 获取单个班级详情（多表 JOIN + LEFT JOIN，6 行内）—— 命中规则 1 多表 JOIN → 注意：移到 XML 需统一迁移，此处保持现状以缩小本次改动范围。
    // 备注：该方法已为多表 JOIN，应在后续重构中一并迁移到 XML。本次 §7.4 迁移聚焦含动态 SQL / UNION / CASE / foreach 的方法。
    @Select("SELECT cc.id, cc.class_name as className, cc.year, cc.capacity, cc.course_id as courseId, c.course_name as courseName, cc.teacher_id as teacherId, u.name as teacherName, cc.major_id as majorId, m.major_name as majorName FROM course_classes cc LEFT JOIN courses c ON cc.course_id = c.id JOIN users u ON cc.teacher_id = u.id LEFT JOIN majors m ON cc.major_id = m.id WHERE cc.id = #{classId}")
    Map<String, Object> getClassById(Long classId);
    Integer countManagedClasses(@Param("teacherId") Long teacherId, @Param("classId") Long classId);

    // 课程分配相关方法
    List<Map<String, Object>> getClassAssignments(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId, @Param("classId") Long classId);
    Integer countClassAssignments(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId, @Param("classId") Long classId);
    Integer countManagedAssignments(@Param("teacherId") Long teacherId, @Param("assignmentId") Long assignmentId);
    Map<String, Object> checkCourseAssignmentByClassId(@Param("classId") Long classId, @Param("courseId") Long courseId);
    Map<String, Object> checkCourseAssignmentByClassName(@Param("className") String className, @Param("courseId") Long courseId);
    Map<String, Object> checkClassCourseAssignment(@Param("classId") Long classId, @Param("courseId") Long courseId);
    void updateCourseAssignment(Map<String, Object> assignData);
    void assignCourse(Map<String, Object> assignData);
    void unassignCourse(Long assignmentId);
    void unassignClassCourse(Map<String, Object> params);
    List<Map<String, Object>> getCoursesByClassId(@Param("classId") Long classId);

    // 删除关联记录的方法（用于取消课程分配时清理外键约束）
    void deleteAssignmentClassesByClassId(Long classId);
    void deleteClassStudentsByClassId(Long classId);
    void deleteExamClassesByClassId(Long classId);

    /**
     * 获取课程的所有知识点
     */
    @Select("SELECT id, knowledge_point_name, description, difficulty_level " +
            "FROM knowledge_points " +
            "WHERE course_id = #{courseId}")
    List<Map<String, Object>> getCourseKnowledgePoints(@Param("courseId") Long courseId);

    /**
     * 获取学生所在的所有班级（多表 JOIN，保留注解，列表式字符串）—— 备注：此方法已涉及 4 表 JOIN，
     * 按 §7.4 规则应迁移到 XML，但本次 §7.2 聚焦含动态 SQL/CASE/UNION/foreach 的方法。
     */
    @Select("SELECT cc.id, cc.class_name as className, cc.year, cc.major_id as majorId, " +
            "m.major_name as majorName, cc.teacher_id as teacherId, u.name as teacherName " +
            "FROM class_students cs " +
            "JOIN course_classes cc ON cs.class_id = cc.id " +
            "LEFT JOIN majors m ON cc.major_id = m.id " +
            "LEFT JOIN users u ON cc.teacher_id = u.id " +
            "WHERE cs.student_id = #{studentId}")
    List<Map<String, Object>> getClassesByStudentId(@Param("studentId") Long studentId);
}
