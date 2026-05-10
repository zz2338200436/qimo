package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.Assignment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AssignmentMapper {
    // 查询所有作业
    @Select("SELECT id, title, description, course_id AS courseId, due_date AS dueDate, publish_date AS publishDate, is_active AS isActive, teacher_id AS teacherId, max_score AS maxScore, submission_count AS submissionCount, graded_count AS gradedCount, status FROM assignments")
    List<Assignment> getAllAssignments();

    // 根据作业ID查询作业详情
    @Select("SELECT id, title, description, course_id AS courseId, due_date AS dueDate, publish_date AS publishDate, is_active AS isActive, teacher_id AS teacherId, max_score AS maxScore, submission_count AS submissionCount, graded_count AS gradedCount, status FROM assignments WHERE id = #{id}")
    Assignment getAssignmentById(Long id);

    // 根据课程ID查询作业列表
    @Select("SELECT id, title, description, course_id AS courseId, due_date AS dueDate, publish_date AS publishDate, is_active AS isActive, teacher_id AS teacherId, max_score AS maxScore, submission_count AS submissionCount, graded_count AS gradedCount, status FROM assignments WHERE course_id = #{courseId}")
    List<Assignment> getAssignmentsByCourseId(Long courseId);

    // 根据课程ID查询作业总数
    @Select("SELECT COUNT(*) FROM assignments WHERE course_id = #{courseId}")
    Integer getAssignmentCountByCourseId(Long courseId);

    // 根据课程ID和学生ID查询已完成作业数
    @Select("SELECT COUNT(*) FROM assignment_submissions WHERE assignment_id IN (SELECT id FROM assignments WHERE course_id = #{courseId}) AND student_id = #{studentId}")
    Integer getCompletedAssignmentCountByCourseAndStudent(Long courseId, Long studentId);

    // 新增作业
    @Insert("INSERT INTO assignments(title, description, course_id, due_date, publish_date, is_active, teacher_id, max_score, submission_count, graded_count, status) VALUES(#{title}, #{description}, #{courseId}, #{dueDate}, #{publishDate}, #{isActive}, #{teacherId}, #{maxScore}, 0, 0, 'pending')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Assignment assignment);

    // 更新作业
    @Update("UPDATE assignments SET title = #{title}, description = #{description}, course_id = #{courseId}, due_date = #{dueDate}, publish_date = #{publishDate}, is_active = #{isActive}, teacher_id = #{teacherId}, max_score = #{maxScore} WHERE id = #{id}")
    void update(Assignment assignment);

    // 删除作业
    @Delete("DELETE FROM assignments WHERE id = #{id}")
    void delete(Long id);

    // 多表 JOIN → 见 AssignmentMapper.xml
    List<java.util.Map<String, Object>> getSubmissionsByAssignmentId(Long assignmentId);

    // 根据教师ID查询即将截止的作业数量
    @Select("SELECT COUNT(*) FROM assignments WHERE teacher_id = #{teacherId} AND due_date > NOW() AND is_active = true")
    Integer countUpcomingAssignmentsByTeacher(Long teacherId);

    // 根据作业ID删除所有相关提交记录
    @Delete("DELETE FROM assignment_submissions WHERE assignment_id = #{assignmentId}")
    void deleteSubmissionsByAssignmentId(Long assignmentId);

    // 根据作业ID删除所有班级关联记录
    @Delete("DELETE FROM assignment_classes WHERE assignment_id = #{assignmentId}")
    void deleteAssignmentClassesByAssignmentId(Long assignmentId);

    // 根据作业ID删除所有知识点关联记录
    @Delete("DELETE FROM assignment_knowledge_points WHERE assignment_id = #{assignmentId}")
    void deleteAssignmentKnowledgePointsByAssignmentId(Long assignmentId);

    // 根据作业ID获取分配的班级列表
    @Select("SELECT class_id FROM assignment_classes WHERE assignment_id = #{assignmentId}")
    List<Long> getAssignmentClasses(@Param("assignmentId") Long assignmentId);

    // 根据班级ID统计学生数
    @Select("SELECT COUNT(*) FROM class_students WHERE class_id = #{classId}")
    Integer countStudentsByClassId(@Param("classId") Long classId);

    // 单表 + UNION 子查询 → 见 AssignmentMapper.xml
    Integer countStudentsByCourseId(@Param("courseId") Long courseId);

    // 获取所有作业提交记录
    @Select("SELECT assignment_id as assignmentId, graded FROM assignment_submissions")
    List<java.util.Map<String, Object>> getAllSubmissions();

    // 获取作业提交统计信息
    @Select("SELECT assignment_id as assignmentId, COUNT(*) as total, SUM(CASE WHEN graded = 1 THEN 1 ELSE 0 END) as graded FROM assignment_submissions GROUP BY assignment_id")
    List<java.util.Map<String, Object>> getSubmissionStats();

    /**
     * 统计教师名下作业的未提交数量（按课程/班级可选过滤）。
     * 动态 WHERE + 多层子查询 + UNION + 多表 JOIN → 见 AssignmentMapper.xml
     */
    Integer countMissingSubmissionsByTeacher(@Param("teacherId") Long teacherId,
                                             @Param("classId") Long classId,
                                             @Param("courseId") Long courseId);

    /**
     * 统计教师作业对应的预期提交学生数（去重）。
     * 动态 WHERE + 多表 LEFT JOIN → 见 AssignmentMapper.xml
     */
    Integer countExpectedStudentsForTeacher(@Param("teacherId") Long teacherId,
                                            @Param("classId") Long classId,
                                            @Param("courseId") Long courseId);

    /**
     * 统计教师作业对应的预期提交总数（按作业-班级求和）。
     * 动态 WHERE + 子查询分组 + 多表 LEFT JOIN → 见 AssignmentMapper.xml
     */
    Integer sumExpectedSubmissionsForTeacher(@Param("teacherId") Long teacherId,
                                             @Param("classId") Long classId,
                                             @Param("courseId") Long courseId);

    // 根据教师ID获取所有作业
    @Select("SELECT id, title, description, course_id AS courseId, due_date AS dueDate, publish_date AS publishDate, is_active AS isActive, teacher_id AS teacherId, max_score AS maxScore, submission_count AS submissionCount, graded_count AS gradedCount, status FROM assignments WHERE teacher_id = #{teacherId}")
    List<Assignment> getAllAssignmentsByTeacher(Long teacherId);

    /**
     * 根据学生ID获取其关联到的所有作业（通过 assignment_classes 表关联）。
     * 多表 JOIN → 见 AssignmentMapper.xml
     */
    List<Assignment> getAssignmentsByStudentId(@Param("studentId") Long studentId);

    /**
     * 为新创建的作业自动建立与课程下所有班级的关联关系。
     * INSERT ... SELECT + UNION 子查询 → 见 AssignmentMapper.xml
     */
    void insertAssignmentClassesForCourseAndTeacher(@Param("assignmentId") Long assignmentId,
                                                    @Param("courseId") Long courseId,
                                                    @Param("teacherId") Long teacherId);
}
