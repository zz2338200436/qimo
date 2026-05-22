package com._202510007517.platform.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface KnowledgeMasteryJpaRepository
        extends JpaRepository<KnowledgeMasteryEntity, Long>, JpaSpecificationExecutor<KnowledgeMasteryEntity> {

    Optional<KnowledgeMasteryEntity> findByStudentIdAndCourseIdAndKnowledgePointId(
            Long studentId,
            Long courseId,
            Long knowledgePointId);
}
