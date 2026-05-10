package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.AssignmentSubmission;

import java.util.List;
import java.util.Map;

public interface AssignmentSubmissionService {
    AssignmentSubmission submitAssignment(Long assignmentId, Long studentId, String content);
    AssignmentSubmission getSubmissionByAssignmentAndStudent(Long assignmentId, Long studentId);
    boolean gradeAssignment(Long submissionId, Integer score, String teacherComment);
    AssignmentSubmission getSubmissionById(Long submissionId);
    List<AssignmentSubmission> getAllSubmissions();
    List<AssignmentSubmission> getSubmissionsByAssignmentId(Long assignmentId);
    List<AssignmentSubmission> getSubmissionsByStudentId(Long studentId);
    boolean deleteSubmission(Long submissionId);
    boolean updateSubmission(AssignmentSubmission submission);
    List<AssignmentSubmission> getSubmissionsWithPagination(Integer page, Integer size, String sortBy, String order, Long assignmentId, Long studentId, Boolean graded);
    Integer countSubmissions(Long assignmentId, Long studentId, Boolean graded);

}