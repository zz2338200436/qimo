package com._202510007517.platform.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamSubmissionJpaRepository extends JpaRepository<ExamSubmissionEntity, Long> {

    Optional<ExamSubmissionEntity> findByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamSubmissionEntity> findByExamIdOrderBySubmissionDateDescIdDesc(Long examId);

    List<ExamSubmissionEntity> findByExamIdIn(Collection<Long> examIds);

    List<ExamSubmissionEntity> findByStudentIdAndGradedTrueOrderBySubmissionDateDescIdDesc(Long studentId);

    long countByExamId(Long examId);

    void deleteByExamId(Long examId);
}
