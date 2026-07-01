package com._202510007517.platform.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamQuestionJpaRepository extends JpaRepository<ExamQuestionEntity, Long> {

    List<ExamQuestionEntity> findByExamIdOrderBySortOrderAscIdAsc(Long examId);

    void deleteByExamId(Long examId);
}
