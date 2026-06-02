package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.AssignmentSubmission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.Map;

@Mapper
public interface AssignmentSubmissionMapper {
    @Insert("INSERT INTO assignment_submissions (graded, is_late, late_penalty, score, submission_date, teacher_comment, assignment_id, student_id, content) VALUES (#{graded}, #{isLate}, #{latePenalty}, #{score}, #{submissionDate}, #{teacherComment}, #{assignmentId}, #{studentId}, #{content})")
    int insertSubmission(AssignmentSubmission submission);

    @Select("SELECT * FROM assignment_submissions WHERE assignment_id = #{assignmentId} AND student_id = #{studentId} ORDER BY submission_date DESC LIMIT 1")
    AssignmentSubmission findByAssignmentAndStudent(Long assignmentId, Long studentId);

    @Select("SELECT COUNT(*) FROM assignment_submissions WHERE CAST(graded AS UNSIGNED) = 0 AND assignment_id IN (SELECT id FROM assignments WHERE teacher_id = #{teacherId})")
    Integer countUngradedSubmissionsByTeacher(Long teacherId);

    @Select("SELECT DATE_FORMAT(submission_date, '%Y-%m-%d') AS submission_date, COUNT(*) AS submission_count FROM assignment_submissions WHERE assignment_id IN (SELECT id FROM assignments WHERE teacher_id = #{teacherId}) AND submission_date >= DATE_SUB(NOW(), INTERVAL 6 DAY) GROUP BY DATE_FORMAT(submission_date, '%Y-%m-%d') ORDER BY submission_date")
    List<Map<String, Object>> getSubmissionCountsByDay(Long teacherId);

    @Select("SELECT DATE_FORMAT(due_date, '%Y-%m-%d') AS due_date, COUNT(*) AS total_assignments FROM assignments WHERE teacher_id = #{teacherId} AND due_date >= DATE_SUB(NOW(), INTERVAL 6 DAY) GROUP BY DATE_FORMAT(due_date, '%Y-%m-%d') ORDER BY due_date")
    List<Map<String, Object>> getTotalAssignmentsByDay(Long teacherId);

    @Select("SELECT * FROM assignment_submissions WHERE id = #{id}")
    AssignmentSubmission findById(Long id);

    @Update("UPDATE assignment_submissions SET score = #{score}, teacher_comment = #{teacherComment}, graded = #{graded}, content = #{content} WHERE id = #{id}")
    int updateSubmission(AssignmentSubmission submission);

    @Select("SELECT * FROM assignment_submissions")
    List<AssignmentSubmission> findAll();

    @Select("SELECT * FROM assignment_submissions WHERE assignment_id = #{assignmentId}")
    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);

    @Select("SELECT * FROM assignment_submissions WHERE student_id = #{studentId}")
    List<AssignmentSubmission> findByStudentId(Long studentId);

    @Delete("DELETE FROM assignment_submissions WHERE id = #{submissionId}")
    int deleteSubmission(Long submissionId);

    @Update("UPDATE assignment_submissions SET graded = #{graded}, is_late = #{isLate}, late_penalty = #{latePenalty}, score = #{score}, submission_date = #{submissionDate}, teacher_comment = #{teacherComment}, assignment_id = #{assignmentId}, student_id = #{studentId}, content = #{content} WHERE id = #{id}")
    int fullUpdateSubmission(AssignmentSubmission submission);

    // 多表 JOIN + 动态 WHERE → 见 AssignmentSubmissionMapper.xml
    List<AssignmentSubmission> findWithPagination(Long assignmentId, Long studentId, Boolean graded, String sortBy, String order, Integer offset, Integer limit);

    // 动态 WHERE → 见 AssignmentSubmissionMapper.xml
    Integer countSubmissions(Long assignmentId, Long studentId, Boolean graded);

    // 多表 JOIN + 动态 WHERE → 见 AssignmentSubmissionMapper.xml
    List<Map<String, Object>> getAssignmentScoreTrend(@Param("teacherId") Long teacherId,
                                                      @Param("classId") Long classId,
                                                      @Param("courseId") Long courseId,
                                                      @Param("studentId") Long studentId,
                                                      @Param("startDate") java.util.Date startDate,
                                                      @Param("endDate") java.util.Date endDate);

    /**
     * 获取学生作业提交统计信息。
     * 多表 LEFT JOIN → 见 AssignmentSubmissionMapper.xml
     */
    Map<String, Object> getStudentSubmissionStats(@Param("studentId") Long studentId,
                                                   @Param("courseId") Long courseId,
                                                   @Param("days") int days);

    /**
     * 获取教师课程下最近的作业提交记录（用于仪表盘最近活动）。
     * 多表 JOIN → 见 AssignmentSubmissionMapper.xml
     */
    List<Map<String, Object>> getRecentSubmissionsByTeacher(@Param("teacherId") Long teacherId, @Param("limit") int limit);

    /**
     * 获取教师最近批改的作业记录（用于仪表盘最近活动）。
     * 多表 JOIN → 见 AssignmentSubmissionMapper.xml
     */
    List<Map<String, Object>> getRecentGradedByTeacher(@Param("teacherId") Long teacherId, @Param("limit") int limit);

    /**
     * 获取教师历史上单日最大提交数（用于计算相对提交率）。
     * 子查询 + 多表 JOIN → 见 AssignmentSubmissionMapper.xml
     */
    Integer getMaxDailySubmissionsByTeacher(@Param("teacherId") Long teacherId);
}
