package com._202510007517.platform.agent.assistant;

import com._202510007517.platform.agent.service.AgentStreamingCallback;

public interface AssistantConversationService {

    String reply(AgentAssistantType type, AgentAssistantRequest request);

    default String replyStream(AgentAssistantType type,
                               AgentAssistantRequest request,
                               AgentStreamingCallback callback) throws Exception {
        String reply = reply(type, request);
        if (reply != null && callback != null) {
            callback.onPartialResponse(reply);
        }
        return reply;
    }
}
