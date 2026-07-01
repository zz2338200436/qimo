package com._202510007517.platform.agent.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantMemoryKeyFactoryTest {

    private final AssistantMemoryKeyFactory factory = new AssistantMemoryKeyFactory();

    @Test
    void buildsScopedMemoryKeyFromSessionAndAssistantType() {
        String key = factory.build("42", AgentAssistantType.GENERAL);

        assertThat(key).isEqualTo("agent-session:42:assistant:GENERAL");
    }

    @Test
    void fallsBackWhenSessionIdMissing() {
        String key = factory.build(null, AgentAssistantType.KNOWLEDGE);

        assertThat(key).isEqualTo("agent-session:anonymous:assistant:KNOWLEDGE");
    }

    @Test
    void normalizesBlankSessionIdToAnonymous() {
        String key = factory.build("   ", AgentAssistantType.KNOWLEDGE);

        assertThat(key).isEqualTo("agent-session:anonymous:assistant:KNOWLEDGE");
    }
}
