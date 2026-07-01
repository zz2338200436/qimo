package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.ExamSubmission;
import java.util.List;
import java.util.Map;

public interface ExamSubmissionService {
    ExamSubmission submitExam(Long examId, Long studentId, Integer timeTaken, Map<String, String> answers);
    ExamSubmission getSubmissionByExamAndStudent(Long examId, Long studentId);
    ExamSubmission gradeExam(Long submissionId, Integer score, String teacherComment);
    List<ExamSubmission> getSubmissionsByExamId(Long examId);
    List<ExamSubmission> getAllSubmissions();
    ExamSubmission getSubmissionById(Long submissionId);
    List<ExamSubmission> getSubmissionsByStudentId(Long studentId);
    boolean deleteSubmission(Long submissionId);
    boolean updateSubmission(ExamSubmission submission);
    List<ExamSubmission> getSubmissionsWithPagination(Integer page, Integer size, Integer total, String sortBy, String order, Long examId, Long studentId, Boolean graded);
    Integer countSubmissions(Long examId, Long studentId, Boolean graded);
}
