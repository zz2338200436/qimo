package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentChatRequestDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentChatStreamingService {
    SseEmitter streamChat(Long userId, String userRole, AgentChatRequestDTO request);

    SseEmitter streamChat(Long userId, String userRole, String sessionId, String message);

    AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message);

    AgentChatResponseDTO chat(AgentChatRequestDTO request, Long userId, String userRole);
}
