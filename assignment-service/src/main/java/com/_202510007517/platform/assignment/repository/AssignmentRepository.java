package com._202510007517.platform.assignment.repository;

import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository {
    Optional<AssignmentRecord> findById(Long assignmentId);

    List<AssignmentRecord> findByCourseId(Long courseId);

    List<AssignmentRecord> findByTeacherId(Long teacherId);

    List<AssignmentRecord> findByStudentId(Long studentId);

    List<AssignmentRecord> findByClassIds(List<Long> classIds);

    List<AssignmentSubmissionRecord> findSubmissionsByAssignmentId(Long assignmentId);

    Optional<AssignmentSubmissionRecord> findSubmissionByAssignmentAndStudent(Long assignmentId, Long studentId);

    List<AssignmentSubmissionRecord> findSubmissionsByStudentId(Long studentId);

    List<AssignmentSubmissionRecord> findGradedSubmissionsByStudentId(Long studentId);

    Optional<AssignmentSubmissionRecord> findSubmissionById(Long submissionId);

    AssignmentSubmissionRecord insertSubmission(AssignmentSubmissionRecord submission);

    void updateSubmission(AssignmentSubmissionRecord submission);

    void updateAssignment(AssignmentRecord assignment);

    AssignmentRecord insertAssignment(AssignmentRecord assignment);

    void updateAssignmentDetails(AssignmentRecord assignment);

    void replaceAssignmentClasses(Long assignmentId, List<Long> classIds);

    List<Long> findKnowledgePointIdsByAssignmentId(Long assignmentId);

    void replaceAssignmentKnowledgePoints(Long assignmentId, List<Long> knowledgePointIds);

    void deleteAssignmentCascade(Long assignmentId);
}
