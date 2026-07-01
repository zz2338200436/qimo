package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.assistant.AssistantConversationService;
import com._202510007517.platform.agent.assistant.FallbackAssistantConversationService;
import com._202510007517.platform.agent.assistant.GeneralAssistant;
import com._202510007517.platform.agent.assistant.KnowledgeAssistant;
import com._202510007517.platform.agent.assistant.LangChain4jAssistantConversationService;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentAssistantConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    GeneralAssistant generalAssistant(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        return AiServices.builder(GeneralAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    KnowledgeAssistant knowledgeAssistant(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        return AiServices.builder(KnowledgeAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    AssistantConversationService langChain4jAssistantConversationService(ObjectProvider<GeneralAssistant> generalAssistantProvider,
                                                                         KnowledgeAssistant knowledgeAssistant,
                                                                         RagKnowledgeService ragKnowledgeService) {
        return new LangChain4jAssistantConversationService(
                generalAssistantProvider.getIfAvailable(),
                knowledgeAssistant,
                ragKnowledgeService);
    }

    @Bean
    @ConditionalOnMissingBean(AssistantConversationService.class)
    AssistantConversationService fallbackAssistantConversationService() {
        return new FallbackAssistantConversationService();
    }
}
