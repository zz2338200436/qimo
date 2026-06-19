package com._202510007517.platform.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRagPropertiesTest {

    @Test
    void bindsRagPropertiesWithDefaultsAndOverrides() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "agent.rag.enabled", "true",
                "agent.rag.document-paths[0]", "docs/rag-knowledge-base.md",
                "agent.rag.embedding-base-url", "http://localhost:11434",
                "agent.rag.embedding-model", "qwen3-embedding:0.6b",
                "agent.rag.max-chunks", "5",
                "agent.rag.min-score", "0.25"
        ));

        AgentRagProperties properties = new Binder(source)
                .bind("agent.rag", Bindable.of(AgentRagProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDocumentPaths()).isEqualTo(List.of("docs/rag-knowledge-base.md"));
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingModel()).isEqualTo("qwen3-embedding:0.6b");
        assertThat(properties.getMaxChunks()).isEqualTo(5);
        assertThat(properties.getMinScore()).isEqualTo(0.25);
    }

    @Test
    void providesSafeDefaults() {
        AgentRagProperties properties = new AgentRagProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getDocumentPaths()).containsExactly("docs/rag-knowledge-base.md");
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingModel()).isEqualTo("qwen3-embedding:0.6b");
        assertThat(properties.getMaxChunks()).isEqualTo(4);
        assertThat(properties.getMinScore()).isEqualTo(0.0);
    }
}
