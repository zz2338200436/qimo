package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.config.AgentLlmProperties;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "agent.llm.smoke.enabled", matches = "true")
@EnabledIfEnvironmentVariable(named = "XIAOMI_API_KEY", matches = ".+")
class LlmIntentRecognitionSmokeTest {

    @Test
    void recognizesIntentThroughConfiguredOpenAiCompatibleModel() {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setEnabled(true);
        properties.setApiKey(System.getenv("XIAOMI_API_KEY"));
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
        LlmIntentRecognitionService service = new LlmIntentRecognitionService(
                chatModel,
                new RuleBasedIntentRecognitionService(),
                new ObjectMapper(),
                properties
        );

        RecognizedIntent result = service.recognize("帮我查看课程列表");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_COURSES);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(properties.getMinimumConfidence());
    }
}
