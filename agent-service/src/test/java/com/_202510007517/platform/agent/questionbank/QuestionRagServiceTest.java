package com._202510007517.platform.agent.questionbank;

import com._202510007517.platform.agent.config.QuestionBankProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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

    @Test
    void returnsRandomQuestionsWhenTopicIsNotProvided() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(20);
        properties.setMinScore(0.0);

        QuestionBankIndex index = new QuestionBankIndex();
        List<QuestionBankIndex.IndexedQuestion> indexedQuestions = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            indexedQuestions.add(new QuestionBankIndex.IndexedQuestion(
                    new QuestionChunk("q" + i, "source.md", "题库", "Java基础", "中等", "SINGLE_CHOICE",
                            List.of(), "content-" + i, List.of("A", "B"), "A", "analysis-" + i, "all", i),
                    List.of(1.0, 0.0)));
        }
        index.replaceAll(indexedQuestions);

        DeterministicRandom random = new DeterministicRandom(11, 3, 7, 1, 9, 4, 0, 10, 6, 2);
        QuestionRagService service = new QuestionRagService(properties, text -> List.of(1.0, 0.0), index, random);

        Map<String, Object> result = service.generateQuestions("TEACHER", null, "中等", 10, null);

        assertThat(result).containsEntry("topic", null);
        assertThat(result).containsEntry("count", 10);
        assertThat(result).containsEntry("actualCount", 10);
        assertThat(result).containsEntry("partial", false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
        assertThat(questions).hasSize(10);
        assertThat(questions).extracting(item -> item.get("id"))
                .doesNotHaveDuplicates()
                .allSatisfy(id -> assertThat(String.valueOf(id)).startsWith("q"));
    }

    @Test
    void treatsGenericRandomTopicAsNoTopicFilter() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(20);
        properties.setMinScore(0.0);

        QuestionBankIndex index = new QuestionBankIndex();
        List<QuestionBankIndex.IndexedQuestion> indexedQuestions = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            indexedQuestions.add(new QuestionBankIndex.IndexedQuestion(
                    new QuestionChunk("q" + i, "source.md", "题库", "分布式基础", "中等", "SINGLE_CHOICE",
                            List.of(), "content-" + i, List.of("A", "B"), "A", "analysis-" + i, "all", i),
                    List.of(1.0, 0.0)));
        }
        index.replaceAll(indexedQuestions);

        QuestionRagService service = new QuestionRagService(properties, text -> List.of(1.0, 0.0), index, new DeterministicRandom(1, 3, 5, 7, 0));

        Map<String, Object> result = service.generateQuestions("TEACHER", "随机", "中等", 5, null);

        assertThat(result).containsEntry("topic", null);
        assertThat(result).containsEntry("actualCount", 5);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
        assertThat(questions).hasSize(5);
    }

    @Test
    void randomRequestsBypassSimilarityThresholdAndStillSampleFromQuestionBank() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(20);
        properties.setMinScore(0.99);

        QuestionBankIndex index = new QuestionBankIndex();
        List<QuestionBankIndex.IndexedQuestion> indexedQuestions = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            indexedQuestions.add(new QuestionBankIndex.IndexedQuestion(
                    new QuestionChunk("q" + i, "source.md", "题库", "分布式基础", "中等", "SINGLE_CHOICE",
                            List.of(), "content-" + i, List.of("A", "B"), "A", "analysis-" + i, "all", i),
                    List.of(0.1, 0.2)));
        }
        index.replaceAll(indexedQuestions);

        QuestionRagService service = new QuestionRagService(
                properties,
                text -> List.of(1.0, 0.0),
                index,
                new DeterministicRandom(4, 2, 1, 0, 3)
        );

        Map<String, Object> result = service.generateQuestions("TEACHER", "随机", "中等", 5, null);

        assertThat(result).containsEntry("topic", null);
        assertThat(result).containsEntry("count", 5);
        assertThat(result).containsEntry("actualCount", 5);
        assertThat(result).containsEntry("partial", false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
        assertThat(questions).hasSize(5);
    }

    @Test
    void fallsBackToFilteredResultsWhenEmbeddingLookupFailsForTopicRequest() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(10);
        properties.setMinScore(0.15);

        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "中等", "SINGLE_CHOICE"), List.of()),
                new QuestionBankIndex.IndexedQuestion(question("q2", "中等", "TRUE_FALSE"), List.of())
        ));

        QuestionRagService service = new QuestionRagService(properties, text -> {
            throw new IllegalStateException("embedding unavailable");
        }, index);

        Map<String, Object> result = service.generateQuestions("TEACHER", "Java基础", "中等", 2);

        assertThat(result).containsEntry("topic", "Java基础");
        assertThat(result).containsEntry("actualCount", 2);
        assertThat(result).containsEntry("partial", false);
        assertThat(result.get("message")).isNull();
    }

    @Test
    void treatsPunctuationOnlyTopicAsNoTopicFilter() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(10);
        properties.setMinScore(0.0);

        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "中等", "SINGLE_CHOICE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "中等", "TRUE_FALSE"), List.of(0.95, 0.0))
        ));

        QuestionRagService service = new QuestionRagService(properties, text -> List.of(1.0, 0.0), index, new DeterministicRandom(1));

        Map<String, Object> result = service.generateQuestions("TEACHER", "”", "中等", 2, null);

        assertThat(result).containsEntry("topic", null);
        assertThat(result).containsEntry("actualCount", 2);
        assertThat(result).containsEntry("partial", false);
    }

    @Test
    void treatsContextOnlyFollowUpTopicAsNoTopicFilter() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(10);
        properties.setMinScore(0.0);

        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "中等", "SINGLE_CHOICE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "中等", "TRUE_FALSE"), List.of(0.95, 0.0))
        ));

        QuestionRagService service = new QuestionRagService(properties, text -> List.of(1.0, 0.0), index, new DeterministicRandom(1));

        Map<String, Object> result = service.generateQuestions("TEACHER", "基于刚才内容", "中等", 2, null);

        assertThat(result).containsEntry("topic", null);
        assertThat(result).containsEntry("actualCount", 2);
        assertThat(result).containsEntry("partial", false);
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

    private static final class DeterministicRandom extends java.util.Random {
        private final Queue<Integer> values;

        private DeterministicRandom(int... values) {
            this.values = new ArrayDeque<>();
            for (int value : values) {
                this.values.add(value);
            }
        }

        @Override
        public int nextInt(int bound) {
            Integer value = values.poll();
            if (value == null) {
                return 0;
            }
            return Math.floorMod(value, bound);
        }
    }
}
