package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.assistant.GeneralAssistant;
import com._202510007517.platform.agent.service.IntentRecognitionService;
import com._202510007517.platform.agent.service.FallbackGeneralChatService;
import com._202510007517.platform.agent.service.AgentDataMaskingPolicy;
import com._202510007517.platform.agent.service.GeneralChatService;
import com._202510007517.platform.agent.service.LlmGeneralChatService;
import com._202510007517.platform.agent.service.LlmIntentRecognitionService;
import com._202510007517.platform.agent.service.RuleBasedIntentRecognitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AgentLlmProperties.class)
public class AgentIntentRecognitionConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    ChatModel agentChatModel(AgentLlmProperties properties) {
        if (!"openai".equalsIgnoreCase(properties.getProvider())) {
            throw new IllegalArgumentException("Unsupported agent.llm.provider: " + properties.getProvider());
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("agent.llm.api-key is required when agent.llm.enabled=true");
        }
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    StreamingChatModel agentStreamingChatModel(AgentLlmProperties properties) {
        if (!"openai".equalsIgnoreCase(properties.getProvider())) {
            throw new IllegalArgumentException("Unsupported agent.llm.provider: " + properties.getProvider());
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("agent.llm.api-key is required when agent.llm.enabled=true");
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    IntentRecognitionService llmIntentRecognitionService(ChatModel agentChatModel,
                                                         RuleBasedIntentRecognitionService fallback,
                                                         ObjectMapper objectMapper,
                                                         AgentLlmProperties properties,
                                                         AgentDataMaskingPolicy dataMaskingPolicy) {
        return new LlmIntentRecognitionService(agentChatModel, fallback, objectMapper, properties, dataMaskingPolicy);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "agent.llm", name = "enabled", havingValue = "true")
    GeneralChatService llmGeneralChatService(GeneralAssistant generalAssistant,
                                             AgentDataMaskingPolicy dataMaskingPolicy) {
        return new LlmGeneralChatService(generalAssistant, dataMaskingPolicy);
    }

    @Bean
    @ConditionalOnMissingBean(GeneralChatService.class)
    GeneralChatService fallbackGeneralChatService() {
        return new FallbackGeneralChatService();
    }
}
