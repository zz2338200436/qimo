package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentMessageDTO;

import java.util.List;

public class FallbackGeneralChatService implements GeneralChatService {
    private static final String FALLBACK_MESSAGE = "暂时还不能处理这个请求。";

    @Override
    public String reply(Long userId, String userRole, String sessionId, String message) {
        return FALLBACK_MESSAGE;
    }

    @Override
    public String reply(Long userId, String userRole, String sessionId, String message,
                        List<AgentMessageDTO> recentMessages) {
        return FALLBACK_MESSAGE;
    }
}
