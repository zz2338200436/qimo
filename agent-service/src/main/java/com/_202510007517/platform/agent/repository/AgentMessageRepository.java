package com._202510007517.platform.agent.repository;

import com._202510007517.platform.agent.domain.AgentMessageEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {
    List<AgentMessageEntity> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    List<AgentMessageEntity> findBySessionIdOrderByCreatedAtDescIdDesc(Long sessionId, Pageable pageable);

    void deleteBySessionId(Long sessionId);

    default List<AgentMessageEntity> findRecentBySessionId(Long sessionId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return findBySessionIdOrderByCreatedAtDescIdDesc(sessionId, PageRequest.of(0, limit));
    }
}
