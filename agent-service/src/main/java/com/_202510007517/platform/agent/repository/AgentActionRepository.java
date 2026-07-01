package com._202510007517.platform.agent.repository;

import com._202510007517.platform.agent.domain.AgentActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentActionRepository extends JpaRepository<AgentActionEntity, Long> {
    List<AgentActionEntity> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
