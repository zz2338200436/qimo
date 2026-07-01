package com._202510007517.platform.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentAttachmentJpaRepository extends JpaRepository<AssessmentAttachmentEntity, Long> {

    List<AssessmentAttachmentEntity> findByAssessmentTypeAndAssessmentIdOrderByIdAsc(String assessmentType,
                                                                                     Long assessmentId);

    void deleteByAssessmentTypeAndAssessmentId(String assessmentType, Long assessmentId);
}
