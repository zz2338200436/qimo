package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.rag.InMemoryRagIndex;
import com._202510007517.platform.agent.rag.RagEmbeddingClient;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRagConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentRagConfiguration.class));

    @Test
    void createsOnlyDisabledRagServiceWhenDisabled() {
        contextRunner
                .withPropertyValues("agent.rag.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(RagKnowledgeService.class);
                    assertThat(context).doesNotHaveBean(RagEmbeddingClient.class);
                    assertThat(context).doesNotHaveBean(InMemoryRagIndex.class);
                });
    }

    @Test
    void createsRagServiceAndIndexWhenEnabled() {
        contextRunner
                .withUserConfiguration(FakeEmbeddingConfiguration.class)
                .withPropertyValues(
                        "agent.rag.enabled=true",
                        "agent.rag.document-paths[0]=docs/rag-knowledge-base.md")
                .run(context -> {
                    assertThat(context).hasSingleBean(RagKnowledgeService.class);
                    assertThat(context).hasSingleBean(InMemoryRagIndex.class);
                });
    }

    @Configuration
    static class FakeEmbeddingConfiguration {
        @Bean
        RagEmbeddingClient ragEmbeddingClient() {
            return text -> List.of(1.0, 0.0);
        }
    }
}
