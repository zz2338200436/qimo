package com._202510007517.platform.agent.rag;

import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OllamaRagEmbeddingClient implements RagEmbeddingClient {
    private final RestClient restClient;
    private final String model;

    public OllamaRagEmbeddingClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            OllamaEmbedResponse response = restClient.post()
                    .uri("/api/embed")
                    .body(Map.of("model", model, "input", text == null ? "" : text))
                    .retrieve()
                    .body(OllamaEmbedResponse.class);
            if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
                throw new IllegalStateException("Ollama embedding response was empty.");
            }
            return response.embeddings().get(0);
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalStateException state
                    && state.getMessage() != null
                    && state.getMessage().startsWith("Ollama embedding")) {
                throw state;
            }
            throw new IllegalStateException("Ollama embedding request failed.", ex);
        }
    }

    private record OllamaEmbedResponse(List<List<Double>> embeddings) {
    }
}
