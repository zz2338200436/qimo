package com._202510007517.platform.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "agent.question-bank")
public class QuestionBankProperties {
    public static final String DEFAULT_DOCUMENT_PATH = "docs/question-bank";

    private boolean enabled = false;
    private List<String> documentPaths = new ArrayList<>(List.of(DEFAULT_DOCUMENT_PATH));
    private String embeddingBaseUrl = "http://localhost:11434";
    private String embeddingModel = "qwen3-embedding:0.6b";
    private int maxCandidates = 50;
    private double minScore = 0.15;
    private boolean allowReload = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDocumentPaths() {
        return documentPaths;
    }

    public void setDocumentPaths(List<String> documentPaths) {
        if (documentPaths == null || documentPaths.isEmpty()) {
            this.documentPaths = new ArrayList<>(List.of(DEFAULT_DOCUMENT_PATH));
            return;
        }
        List<String> configuredDocumentPaths = documentPaths.stream()
                .filter(Objects::nonNull)
                .filter(path -> !path.isBlank())
                .toList();
        this.documentPaths = configuredDocumentPaths.isEmpty()
                ? new ArrayList<>(List.of(DEFAULT_DOCUMENT_PATH))
                : new ArrayList<>(configuredDocumentPaths);
    }

    public String getEmbeddingBaseUrl() {
        return embeddingBaseUrl;
    }

    public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
        this.embeddingBaseUrl = embeddingBaseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = Math.max(1, maxCandidates);
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = Math.max(0.0, minScore);
    }

    public boolean isAllowReload() {
        return allowReload;
    }

    public void setAllowReload(boolean allowReload) {
        this.allowReload = allowReload;
    }
}
