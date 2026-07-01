package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.assistant.GeneralAssistant;
import com._202510007517.platform.agent.service.GeneralChatService;
import com._202510007517.platform.agent.service.LlmGeneralChatService;
import com._202510007517.platform.agent.service.RuleBasedIntentRecognitionService;
import com._202510007517.platform.agent.service.AgentDataMaskingPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentIntentRecognitionConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AgentIntentRecognitionConfiguration.class)
            .withBean(RuleBasedIntentRecognitionService.class)
            .withBean(AgentDataMaskingPolicy.class)
            .withBean(ObjectMapper.class);

    @Test
    void failsFastWhenLlmIsEnabledWithoutApiKey() {
        contextRunner
                .withPropertyValues(
                        "agent.llm.enabled=true",
                        "agent.llm.api-key=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("agent.llm.api-key is required when agent.llm.enabled=true");
                });
    }

    @Test
    void registersLlmGeneralChatServiceWithoutAssistantConversationServiceBean() {
        contextRunner
                .withPropertyValues(
                        "agent.llm.enabled=true",
                        "agent.llm.api-key=test-key")
                .withBean(GeneralAssistant.class, () -> mock(GeneralAssistant.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GeneralChatService.class);
                    assertThat(context.getBean(GeneralChatService.class))
                            .isInstanceOf(LlmGeneralChatService.class);
                });
    }
}
