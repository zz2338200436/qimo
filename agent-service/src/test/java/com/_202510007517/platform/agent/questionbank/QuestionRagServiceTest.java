package com._202510007517.platform.agent.questionbank;

import com._202510007517.platform.agent.config.QuestionBankProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionRagServiceTest {

    @Test
    void refusesWhenQuestionBankIsDisabled() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(false);

        QuestionRagService service = new QuestionRagService(properties, text -> List.of(1.0, 0.0), new QuestionBankIndex());

        Map<String, Object> result = service.generateQuestions("TEACHER", "Java基础", "中等", 2);

        assertThat(result).containsEntry("topic", "Java基础");
        assertThat(result).containsEntry("count", 2);
        assertThat(result).containsEntry("actualCount", 0);
        assertThat(result).containsEntry("partial", false);
        assertThat(result).containsEntry("difficulty", "中等");
        assertThat(result).containsEntry("questions", List.of());
        assertThat(String.valueOf(result.get("message"))).contains("未启用");
    }

    @Test
    void marksPartialWhenNotEnoughQuestionsMatch() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(50);
        properties.setMinScore(0.0);

        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "中等", "SINGLE_CHOICE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "中等", "SINGLE_CHOICE"), List.of(0.9, 0.0))
        ));

        QuestionRagService service = new QuestionRagService(
                properties,
                text -> List.of(1.0, 0.0),
                index
        );

        Map<String, Object> result = service.generateQuestions("TEACHER", "Java基础", "中等", 5);

        assertThat(result).containsEntry("topic", "Java基础");
        assertThat(result).containsEntry("count", 5);
        assertThat(result).containsEntry("actualCount", 2);
        assertThat(result).containsEntry("partial", true);
        assertThat(result).containsEntry("difficulty", "中等");
        assertThat(String.valueOf(result.get("message"))).contains("2/5");
    }

    @Test
    void normalizesDifficultyBuildsFrontendPayloadAndTrimsToRequestedCount() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(10);
        properties.setMinScore(0.0);

        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "中等", "SINGLE_CHOICE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "中等", "TRUE_FALSE"), List.of(0.95, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q3", "困难", "SINGLE_CHOICE"), List.of(1.0, 0.0))
        ));

        CapturingEmbeddingClient embeddingClient = new CapturingEmbeddingClient();
        QuestionRagService service = new QuestionRagService(properties, embeddingClient, index);

        Map<String, Object> result = service.generateQuestions("ROLE_TEACHER", "Java基础", "medium难度", 1, "single_choice");

        assertThat(embeddingClient.lastText).isEqualTo("Java基础 中等 single_choice");
        assertThat(result).containsEntry("difficulty", "中等");
        assertThat(result).containsEntry("count", 1);
        assertThat(result).containsEntry("actualCount", 1);
        assertThat(result).containsEntry("partial", false);
        assertThat(result.get("message")).isNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0)).containsEntry("id", "q1");
        assertThat(questions.get(0)).containsEntry("content", "Java中用于表示继承的关键字是什么？");
        assertThat(questions.get(0)).containsEntry("difficulty", "中等");
        assertThat(questions.get(0)).containsEntry("type", "选择题");
        assertThat(questions.get(0)).containsEntry("score", null);
        assertThat(questions.get(0)).containsEntry("options", List.of("A. import", "B. extends"));
        assertThat(questions.get(0)).containsEntry("answer", "B");
        assertThat(questions.get(0)).containsEntry("analysis", "analysis");
        assertThat(questions.get(0)).containsEntry("knowledgePoints", List.of("Java基础"));
        assertThat(questions.get(0)).containsEntry("sourcePath", "source.md");
    }

    private QuestionChunk question(String id, String difficulty, String type) {
        return new QuestionChunk(id, "source.md", "题库", "Java基础", difficulty, type,
                List.of(), "Java中用于表示继承的关键字是什么？", List.of("A. import", "B. extends"), "B", "analysis", "all", 1);
    }

    private static final class CapturingEmbeddingClient implements QuestionRagService.EmbeddingClient {
        private String lastText;

        @Override
        public List<Double> embed(String text) {
            this.lastText = text;
            return List.of(1.0, 0.0);
        }
    }
}
