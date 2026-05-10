package com._202510007517.major_assignment.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeMasteryMapper {

    /**
     * 插入或更新知识点掌握情况。
     * 单表 UPSERT（ON DUPLICATE KEY UPDATE），≤ 10 行 → 保留注解。
     */
    @Insert("INSERT INTO knowledge_mastery (student_id, knowledge_point_id, mastery_level, last_assessed_date) " +
            "VALUES (#{studentId}, #{knowledgePointId}, #{masteryLevel}, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "mastery_level = VALUES(mastery_level), " +
            "last_assessed_date = NOW()")
    int insertOrUpdate(@Param("studentId") Long studentId,
                      @Param("knowledgePointId") Long knowledgePointId,
                      @Param("masteryLevel") String masteryLevel,
                      @Param("masteryScore") Integer masteryScore);

    /**
     * 根据学生ID和知识点ID查询掌握情况
     */
    @Select("SELECT * FROM knowledge_mastery WHERE student_id = #{studentId} AND knowledge_point_id = #{knowledgePointId}")
    Map<String, Object> findByStudentAndKnowledgePoint(@Param("studentId") Long studentId,
                                                       @Param("knowledgePointId") Long knowledgePointId);

    /**
     * 更新知识点掌握情况
     */
    @Update("UPDATE knowledge_mastery SET mastery_level = #{masteryLevel}, last_assessed_date = NOW() " +
            "WHERE student_id = #{studentId} AND knowledge_point_id = #{knowledgePointId}")
    int update(@Param("studentId") Long studentId,
              @Param("knowledgePointId") Long knowledgePointId,
              @Param("masteryLevel") String masteryLevel,
              @Param("masteryScore") Integer masteryScore);

    /**
     * 查询作业的所有题目及其关联的知识点和学生得分。
     * 多表 LEFT JOIN → 见 KnowledgeMasteryMapper.xml
     */
    List<Map<String, Object>> findQuestionsByAssignment(@Param("submissionId") Long submissionId,
                                                         @Param("assignmentId") Long assignmentId);

    /**
     * 查询考试的所有题目及其关联的知识点和学生得分。
     * 多表 LEFT JOIN → 见 KnowledgeMasteryMapper.xml
     */
    List<Map<String, Object>> findQuestionsByExam(@Param("submissionId") Long submissionId,
                                                    @Param("examId") Long examId);

    /**
     * 获取作业提交的总分
     */
    @Select("SELECT score FROM assignment_submissions WHERE id = #{submissionId}")
    Map<String, Object> getAssignmentSubmissionScore(@Param("submissionId") Long submissionId);

    /**
     * 获取考试提交的总分
     */
    @Select("SELECT score FROM exam_submissions WHERE id = #{submissionId}")
    Map<String, Object> getExamSubmissionScore(@Param("submissionId") Long submissionId);

    /**
     * 获取作业关联的课程ID和教师ID
     */
    @Select("SELECT course_id, teacher_id FROM assignments WHERE id = #{assessmentId}")
    Map<String, Object> getCourseByAssignment(@Param("assessmentId") Long assessmentId);

    /**
     * 获取考试关联的课程ID和教师ID
     */
    @Select("SELECT course_id, teacher_id FROM exams WHERE id = #{assessmentId}")
    Map<String, Object> getCourseByExam(@Param("assessmentId") Long assessmentId);

    /**
     * 获取课程的所有知识点
     */
    @Select("SELECT id FROM knowledge_points WHERE course_id = #{courseId}")
    List<Map<String, Object>> getKnowledgePointsByCourse(@Param("courseId") Long courseId);

    /**
     * 获取作业关联的知识点。
     * 多表 JOIN → 见 KnowledgeMasteryMapper.xml
     */
    List<Map<String, Object>> getKnowledgePointsByAssignment(@Param("assignmentId") Long assignmentId);

    /**
     * 获取考试关联的知识点。
     * 多表 JOIN → 见 KnowledgeMasteryMapper.xml
     */
    List<Map<String, Object>> getKnowledgePointsByExam(@Param("examId") Long examId);
}
