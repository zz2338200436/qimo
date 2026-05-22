package com._202510007517.platform.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentSubmissionJpaRepository extends JpaRepository<AssignmentSubmissionEntity, Long> {

    List<AssignmentSubmissionEntity> findByAssignmentIdOrderBySubmissionDateDescIdDesc(Long assignmentId);

    List<AssignmentSubmissionEntity> findByAssignmentIdAndStudentIdOrderByIdDesc(Long assignmentId, Long studentId);

    List<AssignmentSubmissionEntity> findByStudentIdOrderBySubmissionDateDescIdDesc(Long studentId);

    List<AssignmentSubmissionEntity> findByStudentIdAndGradedTrueAndScoreIsNotNullOrderBySubmissionDateDescIdDesc(Long studentId);

    long countByAssignmentId(Long assignmentId);

    long countByAssignmentIdAndGradedTrue(Long assignmentId);

    void deleteByAssignmentId(Long assignmentId);
}
