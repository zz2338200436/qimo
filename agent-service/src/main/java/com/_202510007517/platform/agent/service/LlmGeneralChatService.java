package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.assistant.GeneralAssistant;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LlmGeneralChatService implements GeneralChatService {
    private final GeneralAssistant generalAssistant;

    public LlmGeneralChatService(GeneralAssistant generalAssistant,
                                 AgentDataMaskingPolicy dataMaskingPolicy) {
        this.generalAssistant = generalAssistant;
    }

    @Override
    public String reply(Long userId, String userRole, String sessionId, String message) {
        return reply(userId, userRole, sessionId, message, List.of());
    }

    @Override
    public String reply(Long userId, String userRole, String sessionId, String message,
                        List<AgentMessageDTO> recentMessages) {
        return generalAssistant.chat(
                userRole,
                com._202510007517.platform.agent.assistant.LangChain4jAssistantConversationService
                        .formatConversationHistory(recentMessages, message),
                message);
    }

    @Override
    public String replyStream(Long userId, String userRole, String sessionId, String message,
                              List<AgentMessageDTO> recentMessages,
                              AgentStreamingCallback callback) throws Exception {
        StringBuilder reply = new StringBuilder();
        CompletableFuture<String> completion = new CompletableFuture<>();
        generalAssistant.chatStream(
                        userRole,
                        com._202510007517.platform.agent.assistant.LangChain4jAssistantConversationService
                                .formatConversationHistory(recentMessages, message),
                        message)
                .onPartialResponse(partialResponse -> {
                    reply.append(partialResponse);
                    try {
                        callback.onPartialResponse(partialResponse);
                    } catch (Exception ex) {
                        completion.completeExceptionally(ex);
                    }
                })
                .onCompleteResponse(response -> completion.complete(reply.toString()))
                .onError(completion::completeExceptionally)
                .start();
        return completion.get();
    }
}
