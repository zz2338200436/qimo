package com._202510007517.platform.ai.repository;

import com._202510007517.platform.ai.domain.AiGenerationEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaAiGenerationRepository implements AiGenerationRepository {

    private final AiGenerationJpaRepository jpaRepository;

    public JpaAiGenerationRepository(AiGenerationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(AiGenerationRecord record) {
        AiGenerationEntity entity = new AiGenerationEntity();
        entity.setUserId(record.userId());
        entity.setUserRole(record.userRole());
        entity.setPromptKey(record.promptKey());
        entity.setRequestType(record.requestType());
        entity.setRequestPayload(record.requestPayload());
        entity.setResponsePayload(record.responsePayload());
        entity.setModelName(record.modelName());
        entity.setStatus(record.status());
        entity.setErrorMessage(record.errorMessage());
        entity.setLatencyMs(record.latencyMs());
        jpaRepository.save(entity);
    }
}
