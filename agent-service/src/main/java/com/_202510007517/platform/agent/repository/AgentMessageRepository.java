package com._202510007517.platform.agent.repository;

import com._202510007517.platform.agent.domain.AgentMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {
    List<AgentMessageEntity> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);
}
