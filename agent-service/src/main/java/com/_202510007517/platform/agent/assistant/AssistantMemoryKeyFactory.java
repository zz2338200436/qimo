package com._202510007517.platform.agent.assistant;

import org.springframework.stereotype.Component;

@Component
public class AssistantMemoryKeyFactory {
    private static final String ANONYMOUS_SESSION_ID = "anonymous";
    private static final String KEY_PREFIX = "agent-session:";
    private static final String ASSISTANT_SEGMENT = ":assistant:";

    public String build(String sessionId, AgentAssistantType type) {
        String safeSessionId = (sessionId == null || sessionId.isBlank()) ? ANONYMOUS_SESSION_ID : sessionId.trim();
        return KEY_PREFIX + safeSessionId + ASSISTANT_SEGMENT + type.name();
    }
}
