package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentMessageDTO;

import java.util.List;

public interface GeneralChatService {
    String reply(Long userId, String userRole, String sessionId, String message);

    default String reply(Long userId, String userRole, String sessionId, String message,
                         List<AgentMessageDTO> recentMessages) {
        return reply(userId, userRole, sessionId, message);
    }

    default String replyStream(Long userId, String userRole, String sessionId, String message,
                               List<AgentMessageDTO> recentMessages,
                               AgentStreamingCallback callback) throws Exception {
        String reply = reply(userId, userRole, sessionId, message, recentMessages);
        if (reply != null && callback != null) {
            callback.onPartialResponse(reply);
        }
        return reply;
    }

    default String reply(Long userId, String userRole, String message) {
        return reply(userId, userRole, null, message);
    }
}
