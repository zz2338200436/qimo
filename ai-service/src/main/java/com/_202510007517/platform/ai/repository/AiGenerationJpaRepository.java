package com._202510007517.platform.ai.repository;

import com._202510007517.platform.ai.domain.AiGenerationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationJpaRepository extends JpaRepository<AiGenerationEntity, Long> {
}
