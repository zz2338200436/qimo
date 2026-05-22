package com._202510007517.platform.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamKnowledgePointJpaRepository extends JpaRepository<ExamKnowledgePointEntity, ExamKnowledgePointId> {

    List<ExamKnowledgePointEntity> findByExamIdOrderByKnowledgePointIdAsc(Long examId);

    void deleteByExamId(Long examId);
}
