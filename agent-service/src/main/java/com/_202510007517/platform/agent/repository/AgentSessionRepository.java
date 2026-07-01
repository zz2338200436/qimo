package com._202510007517.platform.agent.repository;

import com._202510007517.platform.agent.domain.AgentSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentSessionRepository extends JpaRepository<AgentSessionEntity, Long> {
    List<AgentSessionEntity> findByUserIdAndUserRoleIgnoreCaseOrderByUpdatedAtDesc(Long userId, String userRole);

    Optional<AgentSessionEntity> findByIdAndUserIdAndUserRoleIgnoreCase(Long id, Long userId, String userRole);
}
