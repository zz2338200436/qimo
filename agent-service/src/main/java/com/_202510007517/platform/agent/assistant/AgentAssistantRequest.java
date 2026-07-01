package com._202510007517.platform.agent.assistant;

import com._202510007517.platform.agent.api.dto.AgentMessageDTO;

import java.util.List;

public record AgentAssistantRequest(
        Long userId,
        String userRole,
        String sessionId,
        String message,
        List<AgentMessageDTO> recentMessages
) {
}
