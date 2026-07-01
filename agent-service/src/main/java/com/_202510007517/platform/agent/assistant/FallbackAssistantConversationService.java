package com._202510007517.platform.agent.assistant;

public class FallbackAssistantConversationService implements AssistantConversationService {
    private static final String GENERAL_FALLBACK = "暂时还不能处理这个请求。";
    private static final String KNOWLEDGE_FALLBACK = "知识库问答暂未启用。";

    @Override
    public String reply(AgentAssistantType type, AgentAssistantRequest request) {
        if (type == AgentAssistantType.KNOWLEDGE) {
            return KNOWLEDGE_FALLBACK;
        }
        return GENERAL_FALLBACK;
    }
}
