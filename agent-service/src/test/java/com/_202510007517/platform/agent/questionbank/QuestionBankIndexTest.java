package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankIndexTest {

    @Test
    void filtersByRoleAndDifficultyBeforeRanking() {
        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "Java基础", "中等", "teacher", "SINGLE_CHOICE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "Java基础", "困难", "teacher", "SINGLE_CHOICE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q3", "Java基础", "中等", "student", "SINGLE_CHOICE"), List.of(1.0, 0.0))
        ));

        List<QuestionChunk> results = index.search(List.of(1.0, 0.0), "teacher", "Java基础", "中等", null, 10, 0.0);

        assertThat(results).extracting(QuestionChunk::questionId).containsExactly("q1");
    }

    @Test
    void matchesTopicByContainsAndFiltersByType() {
        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "Java基础", "中等", "all", "SINGLE_CHOICE"), List.of(0.8, 0.2)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "Java并发编程", "中等", "all", "TRUE_FALSE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q3", "Spring基础", "中等", "all", "TRUE_FALSE"), List.of(1.0, 0.0))
        ));

        List<QuestionChunk> results = index.search(List.of(1.0, 0.0), "TEACHER", "Java", "中等", "TRUE_FALSE", 10, 0.0);

        assertThat(results).extracting(QuestionChunk::questionId).containsExactly("q2");
    }

    @Test
    void ranksByCosineSimilarityAndAppliesCandidateLimit() {
        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "Java基础", "中等", "all", "SINGLE_CHOICE"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "Java基础", "中等", "all", "SINGLE_CHOICE"), List.of(0.8, 0.2)),
                new QuestionBankIndex.IndexedQuestion(question("q3", "Java基础", "中等", "all", "SINGLE_CHOICE"), List.of(0.0, 1.0))
        ));

        List<QuestionChunk> results = index.search(List.of(1.0, 0.0), "teacher", "Java基础", "中等", null, 2, 0.1);

        assertThat(results).extracting(QuestionChunk::questionId).containsExactly("q1", "q2");
    }

    private QuestionChunk question(String id, String topic, String difficulty, String roleScope, String type) {
        return new QuestionChunk(id, "source.md", "题库", topic, difficulty, type,
                List.of(), "content", List.of("A", "B"), "A", "analysis", roleScope, 1);
    }
}
