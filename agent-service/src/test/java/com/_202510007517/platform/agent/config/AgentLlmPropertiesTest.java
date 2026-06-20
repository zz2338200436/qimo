package com._202510007517.platform.agent.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentLlmPropertiesTest {

    @Test
    void carriesOpenAiCompatibleBaseUrl() {
        AgentLlmProperties properties = new AgentLlmProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("https://token-plan-cn.xiaomimimo.com/v1");

        properties.setBaseUrl("http://localhost:11434/v1");

        assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:11434/v1");
    }

    @Test
    void enablesLangChain4jUnderstandingLayerByDefault() {
        AgentLlmProperties properties = new AgentLlmProperties();

        assertThat(properties.isEnabled()).isTrue();
    }
}
