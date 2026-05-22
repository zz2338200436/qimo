package com._202510007517.platform.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ScoreTrendJpaRepository
        extends JpaRepository<ScoreTrendEntity, Long>, JpaSpecificationExecutor<ScoreTrendEntity> {

    Optional<ScoreTrendEntity> findBySourceTypeAndSubmissionId(String sourceType, Long submissionId);
}
