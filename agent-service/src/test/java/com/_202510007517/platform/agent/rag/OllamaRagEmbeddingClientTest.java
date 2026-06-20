package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaRagEmbeddingClientTest {

    @Test
    void embedsTextThroughOllamaApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/embed"))
                .andExpect(content().json("""
                        {
                          "model": "qwen3-embedding:0.6b",
                          "input": "服务注册"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "embeddings": [[0.1, 0.2, 0.3]]
                        }
                        """, MediaType.APPLICATION_JSON));

        OllamaRagEmbeddingClient client = new OllamaRagEmbeddingClient(
                builder.baseUrl("http://localhost:11434").build(),
                "qwen3-embedding:0.6b");

        List<Double> vector = client.embed("服务注册");

        assertThat(vector).containsExactly(0.1, 0.2, 0.3);
        server.verify();
    }

    @Test
    void throwsClearExceptionWhenOllamaFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        OllamaRagEmbeddingClient client = new OllamaRagEmbeddingClient(
                builder.baseUrl("http://localhost:11434").build(),
                "qwen3-embedding:0.6b");

        assertThatThrownBy(() -> client.embed("服务注册"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ollama embedding request failed");
    }
}
