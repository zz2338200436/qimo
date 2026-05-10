package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.Exam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamMapper {
    // 查询所有考试
    @Select("SELECT id, title, description, course_id, start_time, end_time, publish_date, is_active, is_online, location, duration, teacher_id FROM exams")
    List<Exam> getAllExams();

    // 根据考试ID查询考试详情
    @Select("SELECT id, title, description, course_id, start_time, end_time, publish_date, is_active, is_online, location, duration, teacher_id FROM exams WHERE id = #{id}")
    Exam getExamById(Long id);

    // 根据课程ID查询考试列表
    @Select("SELECT id, title, description, course_id, start_time, end_time, publish_date, is_active, is_online, location, duration, teacher_id FROM exams WHERE course_id = #{courseId}")
    List<Exam> getExamsByCourseId(Long courseId);

    // 根据课程ID查询考试总数
    @Select("SELECT COUNT(*) FROM exams WHERE course_id = #{courseId}")
    Integer getExamCountByCourseId(Long courseId);

    // 根据课程ID和学生ID查询已完成考试数
    @Select("SELECT COUNT(*) FROM exam_submissions WHERE exam_id IN (SELECT id FROM exams WHERE course_id = #{courseId}) AND student_id = #{studentId}")
    Integer getCompletedExamCountByCourseAndStudent(Long courseId, Long studentId);

    // 新增考试
    @Insert("INSERT INTO exams(title, description, course_id, start_time, end_time, publish_date, is_active, is_online, location, duration, teacher_id) VALUES(#{title}, #{description}, #{courseId}, #{startTime}, #{endTime}, #{publishDate}, #{isActive}, #{isOnline}, #{location}, #{duration}, #{teacherId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Exam exam);

    // 更新考试
    @Update("UPDATE exams SET title = #{title}, description = #{description}, course_id = #{courseId}, start_time = #{startTime}, end_time = #{endTime}, publish_date = #{publishDate}, is_active = #{isActive}, is_online = #{isOnline}, location = #{location}, duration = #{duration}, teacher_id = #{teacherId} WHERE id = #{id}")
    void update(Exam exam);

    // 删除考试相关的提交记录
    @Delete("DELETE FROM exam_submissions WHERE exam_id = #{examId}")
    void deleteExamSubmissionsByExamId(@Param("examId") Long examId);

    // 删除考试相关的班级关联记录
    @Delete("DELETE FROM exam_classes WHERE exam_id = #{examId}")
    void deleteExamClassesByExamId(@Param("examId") Long examId);

    // 删除考试
    @Delete("DELETE FROM exams WHERE id = #{id}")
    void delete(Long id);

    @Select("SELECT COUNT(*) FROM exams WHERE teacher_id = #{teacherId} AND end_time > NOW() AND is_active = true")
    Integer countUpcomingExamsByTeacher(Long teacherId);

    // 多表 JOIN → 见 ExamMapper.xml
    List<java.util.Map<String, Object>> getSubmissionsByExamId(Long examId);

    // 根据考试ID统计提交数量
    @Select("SELECT COUNT(*) FROM exam_submissions WHERE exam_id = #{examId}")
    Integer countSubmissionsByExamId(@Param("examId") Long examId);

    // 根据考试ID获取分配的班级列表
    @Select("SELECT class_id FROM exam_classes WHERE exam_id = #{examId}")
    List<Long> getExamClasses(@Param("examId") Long examId);

    // 根据班级ID统计学生数
    @Select("SELECT COUNT(*) FROM class_students WHERE class_id = #{classId}")
    Integer countStudentsByClassId(@Param("classId") Long classId);

    // 单表 + UNION 子查询 → 见 ExamMapper.xml
    Integer countStudentsByCourseId(@Param("courseId") Long courseId);

    /**
     * 根据学生ID获取其关联到的所有考试（通过 exam_classes 表关联）。
     * 多表 JOIN → 见 ExamMapper.xml
     */
    List<Exam> getExamsByStudentId(@Param("studentId") Long studentId);

    /**
     * 为新创建的考试自动建立与课程下所有班级的关联关系。
     * INSERT ... SELECT + UNION 子查询 → 见 ExamMapper.xml
     */
    void insertExamClassesForCourseAndTeacher(@Param("examId") Long examId,
                                              @Param("courseId") Long courseId,
                                              @Param("teacherId") Long teacherId);
}
