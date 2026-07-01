package com._202510007517.platform.agent.assistant;

import com._202510007517.platform.agent.config.AgentAssistantConfiguration;
import com._202510007517.platform.agent.config.AgentRagProperties;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.rag.InMemoryRagIndex;
import com._202510007517.platform.agent.rag.RagChunk;
import com._202510007517.platform.agent.rag.RagEmbeddingClient;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.rag.RagSearchResult;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LangChain4jAssistantConversationServiceTest {
    private static final String GENERAL_HISTORY = "用户：我是张三老师" + System.lineSeparator()
            + "助手：好的，我记住了。";
    private static final String KNOWLEDGE_HISTORY = "用户：上一题你提到了微服务" + System.lineSeparator()
            + "助手：对，下面可以继续问服务注册。";

    private final GeneralAssistant generalAssistant = mock(GeneralAssistant.class);
    private final KnowledgeAssistant knowledgeAssistant = mock(KnowledgeAssistant.class);
    private final RagKnowledgeService ragKnowledgeService = mock(RagKnowledgeService.class);
    private final LangChain4jAssistantConversationService service =
            new LangChain4jAssistantConversationService(
                    generalAssistant,
                    knowledgeAssistant,
                    ragKnowledgeService);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void routesGeneralRequestsToGeneralAssistant() {
        AgentAssistantRequest request = new AgentAssistantRequest(
                7L,
                "TEACHER",
                "12",
                "你是谁",
                List.of(
                        message("USER", "我是张三老师"),
                        message("ASSISTANT", "好的，我记住了。"),
                        message("USER", "你是谁")
                ));
        when(generalAssistant.chat("TEACHER", GENERAL_HISTORY, "你是谁"))
                .thenReturn("我是教学管理平台内置的 AI 助手。");

        String answer = service.reply(AgentAssistantType.GENERAL, request);

        assertThat(answer).isEqualTo("我是教学管理平台内置的 AI 助手。");
    }

    @Test
    void routesKnowledgeRequestsWithRetrievedContext() {
        AgentAssistantRequest request = new AgentAssistantRequest(
                8L,
                "STUDENT",
                "34",
                "什么是服务注册",
                List.of(
                        message("USER", "上一题你提到了微服务"),
                        message("ASSISTANT", "对，下面可以继续问服务注册。"),
                        message("USER", "什么是服务注册")
                ));
        List<RagSearchResult> retrieved = List.of(
                new RagSearchResult(
                        new RagChunk(
                                "chunk-1",
                                "doc-1",
                                "微服务知识库",
                                "服务注册",
                                "docs/rag/system-platform-knowledge.md",
                                "all",
                                null,
                                "服务注册用于让服务实例被发现。",
                                1),
                        0.91));
        when(ragKnowledgeService.retrieve(8L, "STUDENT", "什么是服务注册"))
                .thenReturn(new RagKnowledgeService.Retrieval(retrieved, RagKnowledgeService.RetrievalStatus.READY));
        when(ragKnowledgeService.buildContext(retrieved))
                .thenReturn("[来源: 微服务知识库 / 服务注册]\n服务注册用于让服务实例被发现。");
        when(knowledgeAssistant.answer(
                "STUDENT",
                "什么是服务注册",
                KNOWLEDGE_HISTORY,
                "[来源: 微服务知识库 / 服务注册]\n服务注册用于让服务实例被发现。"))
                .thenReturn("服务注册用于让服务实例被发现。");

        String answer = service.reply(AgentAssistantType.KNOWLEDGE, request);

        assertThat(answer).isEqualTo("服务注册用于让服务实例被发现。");
        verify(ragKnowledgeService).retrieve(8L, "STUDENT", "什么是服务注册");
        verify(ragKnowledgeService).buildContext(retrieved);
    }

    @Test
    void returnsInsufficientEvidenceMessageWhenNoKnowledgeContextCanBeRetrieved() {
        AgentAssistantRequest request = new AgentAssistantRequest(8L, "STUDENT", "34", "查不到的知识", List.of());
        when(ragKnowledgeService.retrieve(8L, "STUDENT", "查不到的知识"))
                .thenReturn(new RagKnowledgeService.Retrieval(List.of(), RagKnowledgeService.RetrievalStatus.READY));
        when(ragKnowledgeService.insufficientEvidenceAnswer())
                .thenReturn("没有在知识库中找到足够依据，暂时无法基于资料回答这个问题。");

        String answer = service.reply(AgentAssistantType.KNOWLEDGE, request);

        assertThat(answer).contains("没有在知识库中找到足够依据");
        verify(ragKnowledgeService).insufficientEvidenceAnswer();
    }

    @Test
    void returnsUnavailableMessageWhenKnowledgeRetrievalFails() {
        AgentAssistantRequest request = new AgentAssistantRequest(8L, "STUDENT", "34", "服务为什么不可用", List.of());
        when(ragKnowledgeService.retrieve(8L, "STUDENT", "服务为什么不可用"))
                .thenReturn(new RagKnowledgeService.Retrieval(List.of(), RagKnowledgeService.RetrievalStatus.FAILED));
        when(ragKnowledgeService.unavailableAnswer())
                .thenReturn("知识库暂时不可用，请稍后再试。");

        String answer = service.reply(AgentAssistantType.KNOWLEDGE, request);

        assertThat(answer).isEqualTo("知识库暂时不可用，请稍后再试。");
        verify(ragKnowledgeService).unavailableAnswer();
    }

    @Test
    void registersFallbackAssistantConversationServiceWhenChatModelIsMissing() {
        contextRunner
                .withUserConfiguration(AssistantSupportTestConfiguration.class, AgentAssistantConfiguration.class)
                .run(context -> {
            assertThat(context).hasSingleBean(AssistantConversationService.class);
            assertThat(context).getBean(AssistantConversationService.class)
                    .isInstanceOf(FallbackAssistantConversationService.class);
            assertThat(context).doesNotHaveBean(LangChain4jAssistantConversationService.class);
            assertThat(context).doesNotHaveBean(GeneralAssistant.class);
            assertThat(context).doesNotHaveBean(KnowledgeAssistant.class);
        });
    }

    @Test
    void registersLangChain4jAssistantConversationServiceWhenDependenciesExist() {
        contextRunner
                .withPropertyValues("agent.llm.enabled=true")
                .withUserConfiguration(
                        ChatModelTestConfiguration.class,
                        AssistantSupportTestConfiguration.class,
                        AgentAssistantConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AssistantConversationService.class);
                    assertThat(context).getBean(AssistantConversationService.class)
                            .isInstanceOf(LangChain4jAssistantConversationService.class);
                    assertThat(context).hasSingleBean(GeneralAssistant.class);
                    assertThat(context).hasSingleBean(KnowledgeAssistant.class);
                });
    }

    @Test
    void failsFastWhenLlmIsEnabledWithoutChatModelEvenIfKnowledgeAssistantExists() {
        contextRunner
                .withPropertyValues("agent.llm.enabled=true")
                .withBean(KnowledgeAssistant.class, () -> mock(KnowledgeAssistant.class))
                .withUserConfiguration(
                        AssistantSupportTestConfiguration.class,
                        AgentAssistantConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("knowledgeAssistant");
                });
    }

    @Configuration
    static class AssistantSupportTestConfiguration {

        @Bean
        RagKnowledgeService ragKnowledgeService() {
            AgentRagProperties properties = new AgentRagProperties();
            properties.setEnabled(true);
            RagEmbeddingClient embeddingClient = text -> List.of(1.0, 0.0);
            return new RagKnowledgeService(properties, embeddingClient, new InMemoryRagIndex(), null);
        }
    }

    @Configuration
    static class ChatModelTestConfiguration {

        @Bean
        ChatModel chatModel() {
            return new ChatModel() {
                @Override
                public String chat(String userMessage) {
                    return "mocked";
                }
            };
        }

        @Bean
        StreamingChatModel streamingChatModel() {
            return mock(StreamingChatModel.class);
        }
    }

    private AgentMessageDTO message(String role, String content) {
        AgentMessageDTO dto = new AgentMessageDTO();
        dto.setRole(role);
        dto.setContent(content);
        return dto;
    }
}
