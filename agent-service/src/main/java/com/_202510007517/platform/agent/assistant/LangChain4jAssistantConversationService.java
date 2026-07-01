package com._202510007517.platform.agent.assistant;

import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.rag.RagSearchResult;
import com._202510007517.platform.agent.service.AgentStreamingCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LangChain4jAssistantConversationService implements AssistantConversationService {
    private static final String GENERAL_FALLBACK = "暂时还不能处理这个请求。";

    private final GeneralAssistant generalAssistant;
    private final KnowledgeAssistant knowledgeAssistant;
    private final RagKnowledgeService ragKnowledgeService;

    public LangChain4jAssistantConversationService(KnowledgeAssistant knowledgeAssistant,
                                                   RagKnowledgeService ragKnowledgeService) {
        this(null, knowledgeAssistant, ragKnowledgeService);
    }

    public LangChain4jAssistantConversationService(GeneralAssistant generalAssistant,
                                                   KnowledgeAssistant knowledgeAssistant,
                                                   RagKnowledgeService ragKnowledgeService) {
        this.generalAssistant = generalAssistant;
        this.knowledgeAssistant = knowledgeAssistant;
        this.ragKnowledgeService = ragKnowledgeService;
    }

    @Override
    public String reply(AgentAssistantType type, AgentAssistantRequest request) {
        return switch (type) {
            case GENERAL -> generalReply(request);
            case KNOWLEDGE -> knowledgeReply(request);
        };
    }

    @Override
    public String replyStream(AgentAssistantType type,
                              AgentAssistantRequest request,
                              AgentStreamingCallback callback) throws Exception {
        return switch (type) {
            case GENERAL -> generalReplyStream(request, callback);
            case KNOWLEDGE -> knowledgeReplyStream(request, callback);
        };
    }

    private String generalReply(AgentAssistantRequest request) {
        if (generalAssistant == null) {
            return GENERAL_FALLBACK;
        }
        return generalAssistant.chat(
                request.userRole(),
                formatConversationHistory(request.recentMessages(), request.message()),
                request.message());
    }

    private String generalReplyStream(AgentAssistantRequest request,
                                      AgentStreamingCallback callback) throws Exception {
        if (generalAssistant == null) {
            if (callback != null) {
                callback.onPartialResponse(GENERAL_FALLBACK);
            }
            return GENERAL_FALLBACK;
        }
        return collectStream(generalAssistant.chatStream(
                request.userRole(),
                formatConversationHistory(request.recentMessages(), request.message()),
                request.message()), callback);
    }

    private String knowledgeReply(AgentAssistantRequest request) {
        RagKnowledgeService.Retrieval retrieval = ragKnowledgeService.retrieve(
                request.userId(),
                request.userRole(),
                request.message());
        if (retrieval.status() == RagKnowledgeService.RetrievalStatus.DISABLED) {
            return ragKnowledgeService.disabledAnswer();
        }
        if (retrieval.status() == RagKnowledgeService.RetrievalStatus.FAILED) {
            return ragKnowledgeService.unavailableAnswer();
        }
        List<RagSearchResult> results = retrieval.results();
        if (results.isEmpty()) {
            return ragKnowledgeService.insufficientEvidenceAnswer();
        }
        return knowledgeAssistant.answer(
                request.userRole(),
                request.message(),
                formatConversationHistory(request.recentMessages(), request.message()),
                ragKnowledgeService.buildContext(results));
    }

    private String knowledgeReplyStream(AgentAssistantRequest request,
                                        AgentStreamingCallback callback) throws Exception {
        RagKnowledgeService.Retrieval retrieval = ragKnowledgeService.retrieve(
                request.userId(),
                request.userRole(),
                request.message());
        if (retrieval.status() == RagKnowledgeService.RetrievalStatus.DISABLED) {
            return emitStatic(callback, ragKnowledgeService.disabledAnswer());
        }
        if (retrieval.status() == RagKnowledgeService.RetrievalStatus.FAILED) {
            return emitStatic(callback, ragKnowledgeService.unavailableAnswer());
        }
        List<RagSearchResult> results = retrieval.results();
        if (results.isEmpty()) {
            return emitStatic(callback, ragKnowledgeService.insufficientEvidenceAnswer());
        }
        return collectStream(knowledgeAssistant.answerStream(
                request.userRole(),
                request.message(),
                formatConversationHistory(request.recentMessages(), request.message()),
                ragKnowledgeService.buildContext(results)), callback);
    }

    private String emitStatic(AgentStreamingCallback callback, String text) throws Exception {
        if (text != null && callback != null) {
            callback.onPartialResponse(text);
        }
        return text;
    }

    private String collectStream(dev.langchain4j.service.TokenStream tokenStream,
                                 AgentStreamingCallback callback) throws Exception {
        StringBuilder reply = new StringBuilder();
        CompletableFuture<String> completion = new CompletableFuture<>();
        tokenStream
                .onPartialResponse(partialResponse -> {
                    reply.append(partialResponse);
                    try {
                        if (callback != null) {
                            callback.onPartialResponse(partialResponse);
                        }
                    } catch (Exception ex) {
                        completion.completeExceptionally(ex);
                    }
                })
                .onCompleteResponse(response -> completion.complete(reply.toString()))
                .onError(completion::completeExceptionally)
                .start();
        return completion.get();
    }

    public static String formatConversationHistory(List<AgentMessageDTO> recentMessages, String currentMessage) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "无";
        }
        List<String> lines = new ArrayList<>();
        for (AgentMessageDTO message : recentMessages) {
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            if (isCurrentUserMessage(message, currentMessage)) {
                continue;
            }
            String roleLabel = "ASSISTANT".equalsIgnoreCase(message.getRole()) ? "助手" : "用户";
            lines.add(roleLabel + "：" + message.getContent().trim());
        }
        return lines.isEmpty() ? "无" : String.join(System.lineSeparator(), lines);
    }

    private static boolean isCurrentUserMessage(AgentMessageDTO message, String currentMessage) {
        return "USER".equalsIgnoreCase(message.getRole())
                && currentMessage != null
                && currentMessage.equals(message.getContent());
    }
}
