package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.ExamSubmission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface ExamSubmissionMapper {
    @Insert("INSERT INTO exam_submissions (graded, score, submission_date, teacher_comment, content, time_taken, exam_id, student_id) VALUES (#{graded}, #{score}, #{submissionDate}, #{teacherComment}, #{content}, #{timeTaken}, #{examId}, #{studentId})")
    int insertSubmission(ExamSubmission submission);

    @Select("SELECT * FROM exam_submissions WHERE exam_id = #{examId} AND student_id = #{studentId}")
    ExamSubmission findByExamAndStudent(Long examId, Long studentId);

    @Select("SELECT * FROM exam_submissions WHERE id = #{submissionId}")
    ExamSubmission findById(Long submissionId);

    @Update("UPDATE exam_submissions SET graded = #{graded}, score = #{score}, teacher_comment = #{teacherComment} WHERE id = #{id}")
    int updateGrade(ExamSubmission submission);

    @Select("SELECT * FROM exam_submissions WHERE exam_id = #{examId}")
    List<ExamSubmission> findByExamId(Long examId);

    @Select("SELECT * FROM exam_submissions")
    List<ExamSubmission> findAll();

    @Select("SELECT * FROM exam_submissions WHERE student_id = #{studentId}")
    List<ExamSubmission> findByStudentId(Long studentId);

    @Delete("DELETE FROM exam_submissions WHERE id = #{submissionId}")
    int deleteSubmission(Long submissionId);

    @Update("UPDATE exam_submissions SET graded = #{graded}, score = #{score}, submission_date = #{submissionDate}, teacher_comment = #{teacherComment}, content = #{content}, time_taken = #{timeTaken}, exam_id = #{examId}, student_id = #{studentId} WHERE id = #{id}")
    int fullUpdateSubmission(ExamSubmission submission);

    // 多表 JOIN + 动态 WHERE → 见 ExamSubmissionMapper.xml
    List<ExamSubmission> findWithPagination(Long examId, Long studentId, Boolean graded, String sortBy, String order, Integer offset, Integer limit);

    // 动态 WHERE → 见 ExamSubmissionMapper.xml
    Integer countSubmissions(Long examId, Long studentId, Boolean graded);

    @Select("SELECT COUNT(*) FROM exam_submissions WHERE graded = 0 AND exam_id IN (SELECT id FROM exams WHERE teacher_id = #{teacherId})")
    Integer countUngradedExamsByTeacher(Long teacherId);

    /**
     * 获取教师课程下最近的考试提交记录（用于仪表盘最近活动）。
     * 多表 JOIN → 见 ExamSubmissionMapper.xml
     */
    List<java.util.Map<String, Object>> getRecentSubmissionsByTeacher(@Param("teacherId") Long teacherId, @Param("limit") int limit);
}
