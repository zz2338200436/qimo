package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.rag.RagKnowledgeService;

import java.util.List;
import java.util.Map;

public record PlannerContext(
        Long userId,
        String userRole,
        AgentSessionEntity session,
        String message,
        List<AgentMessageDTO> recentMessages,
        Map<String, SessionArtifact> artifacts,
        Map<String, Object> pageContext,
        RagKnowledgeService.Retrieval retrieval
) {
}
