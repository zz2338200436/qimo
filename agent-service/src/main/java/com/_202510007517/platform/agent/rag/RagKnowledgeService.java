package com._202510007517.platform.agent.rag;

import com._202510007517.platform.agent.config.AgentRagProperties;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RagKnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(RagKnowledgeService.class);

    public enum RetrievalStatus {
        DISABLED,
        FAILED,
        READY
    }

    public record Retrieval(List<RagSearchResult> results, RetrievalStatus status) {
    }

    private final AgentRagProperties properties;
    private final RagEmbeddingClient embeddingClient;
    private final InMemoryRagIndex index;
    private final ChatModel chatModel;

    public RagKnowledgeService(AgentRagProperties properties,
                               RagEmbeddingClient embeddingClient,
                               InMemoryRagIndex index,
                               ChatModel chatModel) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.index = index;
        this.chatModel = chatModel;
    }

    public String answer(Long userId, String userRole, String question) {
        Retrieval retrieval = retrieve(userId, userRole, question);
        if (retrieval.status() == RetrievalStatus.DISABLED) {
            return disabledAnswer();
        }
        if (retrieval.status() == RetrievalStatus.FAILED) {
            return unavailableAnswer();
        }
        List<RagSearchResult> results = retrieval.results();
        if (results.isEmpty()) {
            return insufficientEvidenceAnswer();
        }
        String generated = generate(question, userRole, results);
        return generated + "\n\n" + sources(results);
    }

    public Retrieval retrieve(Long userId, String userRole, String question) {
        if (!properties.isEnabled()) {
            return new Retrieval(List.of(), RetrievalStatus.DISABLED);
        }
        try {
            List<Double> queryVector = embeddingClient.embed(question);
            return new Retrieval(
                    index.search(queryVector, question, userRole, properties.getMaxChunks(), properties.getMinScore()),
                    RetrievalStatus.READY);
        } catch (RuntimeException ex) {
            log.info("rag answer failed before generation: userId={}, reason={}", userId, ex.getClass().getSimpleName());
            if (!index.isEmpty()) {
                return new Retrieval(
                        index.search(List.of(), question, userRole, properties.getMaxChunks(), properties.getMinScore()),
                        RetrievalStatus.READY);
            }
            return new Retrieval(List.of(), RetrievalStatus.FAILED);
        }
    }

    public String buildContext(List<RagSearchResult> results) {
        return results.stream()
                .map(result -> "[来源: %s / %s]\n%s".formatted(
                        result.chunk().documentTitle(),
                        result.chunk().sectionTitle(),
                        result.chunk().content()))
                .collect(Collectors.joining("\n\n"));
    }

    public String disabledAnswer() {
        return "知识库问答暂未启用。";
    }

    public String unavailableAnswer() {
        return "知识库暂时不可用，请稍后再试。";
    }

    public String insufficientEvidenceAnswer() {
        return "没有在知识库中找到足够依据，暂时无法基于资料回答这个问题。";
    }

    private String generate(String question, String userRole, List<RagSearchResult> results) {
        String context = buildContext(results);
        String prompt = """
                你是教学管理平台内置的 RAG 知识问答助手。
                当前用户角色：%s。

                只能依据下方知识库片段回答。不能编造实时课程、作业、考试、成绩、通知或学生名单。
                如果知识库片段不足以回答，请明确说明没有足够依据。
                回答保持中文、简洁、适合教学平台用户阅读。

                知识库片段：
                %s

                用户问题：
                %s
                """.formatted(userRole == null ? "UNKNOWN" : userRole, context, question == null ? "" : question);
        if (chatModel == null) {
            return fallbackAnswer(results);
        }
        try {
            String answer = chatModel.chat(prompt);
            if (answer == null || answer.isBlank()) {
                return fallbackAnswer(results);
            }
            return answer.trim();
        } catch (RuntimeException ex) {
            log.info("rag chat generation failed: reason={}", ex.getClass().getSimpleName());
            return fallbackAnswer(results);
        }
    }

    private String fallbackAnswer(List<RagSearchResult> results) {
        return results.stream()
                .map(result -> result.chunk().content())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(insufficientEvidenceAnswer());
    }

    private String sources(List<RagSearchResult> results) {
        String sourceText = results.stream()
                .map(result -> "来源：%s / %s".formatted(result.chunk().documentTitle(), result.chunk().sectionTitle()))
                .distinct()
                .collect(Collectors.joining("\n"));
        return sourceText.isBlank() ? "来源：知识库" : sourceText;
    }
}
