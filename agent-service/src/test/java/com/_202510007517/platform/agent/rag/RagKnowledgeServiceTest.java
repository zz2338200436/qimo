package com._202510007517.platform.agent.rag;

import com._202510007517.platform.agent.config.AgentRagProperties;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagKnowledgeServiceTest {

    @Test
    void returnsGeneratedAnswerWithSources() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        properties.setMaxChunks(2);
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        InMemoryRagIndex index = new InMemoryRagIndex();
        RagChunk chunk = new RagChunk(
                "chunk-1",
                "doc-1",
                "RAG 方案",
                "适合回答的问题",
                "docs/rag-knowledge-base.md",
                "all",
                null,
                "RAG 适合回答知识解释类问题。",
                1);
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(chunk, List.of(1.0, 0.0))));
        CapturingChatModel chatModel = new CapturingChatModel("RAG 可以回答知识解释类问题。");

        RagKnowledgeService service = new RagKnowledgeService(properties, embeddingClient, index, chatModel);

        String answer = service.answer(7L, "STUDENT", "什么是 RAG？");

        assertThat(answer).contains("RAG 可以回答知识解释类问题。");
        assertThat(answer).contains("来源：RAG 方案 / 适合回答的问题");
        assertThat(chatModel.prompts).hasSize(1);
        assertThat(chatModel.prompts.get(0)).contains("RAG 适合回答知识解释类问题。");
    }

    @Test
    void returnsUnavailableMessageWhenEmbeddingFails() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        RagEmbeddingClient failingClient = text -> {
            throw new IllegalStateException("Ollama embedding request failed.");
        };

        RagKnowledgeService service = new RagKnowledgeService(
                properties,
                failingClient,
                new InMemoryRagIndex(),
                null);

        String answer = service.answer(7L, "TEACHER", "解释服务注册");

        assertThat(answer).contains("知识库暂时不可用");
    }

    @Test
    void fallsBackToTextSearchWhenQueryEmbeddingFailsButIndexHasChunks() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        InMemoryRagIndex index = new InMemoryRagIndex();
        RagChunk registryChunk = new RagChunk(
                "chunk-1",
                "doc-1",
                "平台知识库",
                "服务注册与发现",
                "docs/rag/system-platform-knowledge.md",
                "all",
                null,
                "注册中心保存服务实例地址、端口、健康状态等元数据。",
                1);
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(registryChunk, List.of())));
        RagEmbeddingClient failingClient = text -> {
            throw new IllegalStateException("Ollama embedding request failed.");
        };
        RagKnowledgeService service = new RagKnowledgeService(properties, failingClient, index, null);

        RagKnowledgeService.Retrieval retrieval = service.retrieve(7L, "TEACHER", "什么是注册中心");

        assertThat(retrieval.status()).isEqualTo(RagKnowledgeService.RetrievalStatus.READY);
        assertThat(retrieval.results())
                .singleElement()
                .satisfies(result -> assertThat(result.chunk().content()).contains("注册中心"));
    }

    @Test
    void returnsInsufficientContextWhenNoChunksMatch() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        properties.setMinScore(0.5);
        RagKnowledgeService service = new RagKnowledgeService(
                properties,
                text -> List.of(1.0, 0.0),
                new InMemoryRagIndex(),
                null);

        String answer = service.answer(7L, "STUDENT", "解释不存在的内容");

        assertThat(answer).contains("没有在知识库中找到足够依据");
    }

    @Test
    void retrieveReturnsMatchingChunksWithoutGeneratingAnswer() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        properties.setMaxChunks(1);
        InMemoryRagIndex index = new InMemoryRagIndex();
        RagChunk chunk = new RagChunk(
                "chunk-1",
                "doc-1",
                "RAG 方案",
                "适合回答的问题",
                "docs/rag-knowledge-base.md",
                "all",
                null,
                "RAG 适合回答知识解释类问题。",
                1);
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(chunk, List.of(1.0, 0.0))));
        RagKnowledgeService service = new RagKnowledgeService(properties, text -> List.of(1.0, 0.0), index, null);

        RagKnowledgeService.Retrieval retrieval = service.retrieve(7L, "STUDENT", "什么是 RAG？");

        assertThat(retrieval.status()).isEqualTo(RagKnowledgeService.RetrievalStatus.READY);
        assertThat(retrieval.results()).hasSize(1);
        assertThat(retrieval.results().get(0).chunk().content()).isEqualTo("RAG 适合回答知识解释类问题。");
    }

    @Test
    void buildContextFormatsRetrievedChunksForAssistantPrompting() {
        RagKnowledgeService service = new RagKnowledgeService(
                new AgentRagProperties(),
                text -> List.of(),
                new InMemoryRagIndex(),
                null);
        List<RagSearchResult> results = List.of(
                new RagSearchResult(
                        new RagChunk(
                                "chunk-1",
                                "doc-1",
                                "平台手册",
                                "登录说明",
                                "docs/rag/system-platform-knowledge.md",
                                "all",
                                null,
                                "学生可通过统一登录入口进入平台。",
                                1),
                        0.88));

        String context = service.buildContext(results);

        assertThat(context).isEqualTo("[来源: 平台手册 / 登录说明]\n学生可通过统一登录入口进入平台。");
    }

    @Test
    void returnsChunkFallbackWithSourcesWhenChatModelIsMissing() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        InMemoryRagIndex index = new InMemoryRagIndex();
        RagChunk chunk = new RagChunk(
                "chunk-1",
                "doc-1",
                "平台手册",
                "登录说明",
                "docs/rag/system-platform-knowledge.md",
                "all",
                null,
                "学生可通过统一登录入口进入平台。",
                1);
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(chunk, List.of(1.0, 0.0))));
        RagKnowledgeService service = new RagKnowledgeService(properties, text -> List.of(1.0, 0.0), index, null);

        String answer = service.answer(7L, "STUDENT", "如何登录平台");

        assertThat(answer).contains("学生可通过统一登录入口进入平台。");
        assertThat(answer).contains("来源：平台手册 / 登录说明");
    }

    @Test
    void fallsBackToChunkContentWhenChatModelReturnsBlank() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        InMemoryRagIndex index = new InMemoryRagIndex();
        RagChunk chunk = new RagChunk(
                "chunk-1",
                "doc-1",
                "平台手册",
                "登录说明",
                "docs/rag/system-platform-knowledge.md",
                "all",
                null,
                "学生可通过统一登录入口进入平台。",
                1);
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(chunk, List.of(1.0, 0.0))));
        RagKnowledgeService service = new RagKnowledgeService(
                properties,
                text -> List.of(1.0, 0.0),
                index,
                new ChatModel() {
                    @Override
                    public String chat(String userMessage) {
                        return "   ";
                    }
                });

        String answer = service.answer(7L, "STUDENT", "如何登录平台");

        assertThat(answer).contains("学生可通过统一登录入口进入平台。");
        assertThat(answer).contains("来源：平台手册 / 登录说明");
    }

    private static class FakeEmbeddingClient implements RagEmbeddingClient {
        @Override
        public List<Double> embed(String text) {
            return List.of(1.0, 0.0);
        }
    }

    private static class CapturingChatModel implements ChatModel {
        private final String answer;
        private final List<String> prompts = new ArrayList<>();

        private CapturingChatModel(String answer) {
            this.answer = answer;
        }

        @Override
        public String chat(String userMessage) {
            prompts.add(userMessage);
            return answer;
        }
    }
}
