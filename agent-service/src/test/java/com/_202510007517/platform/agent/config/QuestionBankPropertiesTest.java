package com._202510007517.platform.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankPropertiesTest {

    @Test
    void bindsConfiguredQuestionBankProperties() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("agent.question-bank.enabled", "true")
                .withProperty("agent.question-bank.document-paths[0]", "docs/question-bank/seeded")
                .withProperty("agent.question-bank.embedding-base-url", "http://localhost:11434")
                .withProperty("agent.question-bank.embedding-model", "qwen3-embedding:0.6b")
                .withProperty("agent.question-bank.max-candidates", "50")
                .withProperty("agent.question-bank.min-score", "0.15")
                .withProperty("agent.question-bank.allow-reload", "true");

        QuestionBankProperties properties = Binder.get(env)
                .bind("agent.question-bank", Bindable.of(QuestionBankProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDocumentPaths()).isEqualTo(List.of("docs/question-bank/seeded"));
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingModel()).isEqualTo("qwen3-embedding:0.6b");
        assertThat(properties.getMaxCandidates()).isEqualTo(50);
        assertThat(properties.getMinScore()).isEqualTo(0.15);
        assertThat(properties.isAllowReload()).isTrue();
    }

    @Test
    void providesSafeDefaults() {
        QuestionBankProperties properties = new QuestionBankProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getDocumentPaths()).containsExactly("docs/question-bank/seeded");
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingModel()).isEqualTo("qwen3-embedding:0.6b");
        assertThat(properties.getMaxCandidates()).isEqualTo(50);
        assertThat(properties.getMinScore()).isEqualTo(0.15);
        assertThat(properties.isAllowReload()).isTrue();
    }

    @Test
    void fallsBackToSafeValuesWhenDefensiveInputsAreInvalid() {
        QuestionBankProperties properties = new QuestionBankProperties();

        properties.setDocumentPaths(List.of("", "   "));
        properties.setMaxCandidates(0);
        properties.setMinScore(-0.1);

        assertThat(properties.getDocumentPaths()).containsExactly("docs/question-bank/seeded");
        assertThat(properties.getMaxCandidates()).isEqualTo(1);
        assertThat(properties.getMinScore()).isEqualTo(0.0);
    }

    @Test
    void filtersNullAndBlankDocumentPaths() {
        QuestionBankProperties properties = new QuestionBankProperties();

        properties.setDocumentPaths(java.util.Arrays.asList(null, "", "docs/custom-question-bank", "   "));

        assertThat(properties.getDocumentPaths()).containsExactly("docs/custom-question-bank");
    }
}
