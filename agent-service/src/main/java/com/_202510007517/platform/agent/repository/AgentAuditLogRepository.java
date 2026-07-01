package com._202510007517.platform.agent.repository;

import com._202510007517.platform.agent.domain.AgentAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentAuditLogRepository extends JpaRepository<AgentAuditLogEntity, Long> {
}
