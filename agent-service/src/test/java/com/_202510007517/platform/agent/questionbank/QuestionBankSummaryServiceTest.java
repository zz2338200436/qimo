package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankSummaryServiceTest {

    @Test
    void summarizesQuestionCountsByTopicAndDifficulty() {
        QuestionBankSummaryService service = new QuestionBankSummaryService(List.of(
                new QuestionChunk("q1", "a.md", "A", "Java基础", "中等", "SINGLE_CHOICE", List.of(), "c1", List.of(), "A", "", "all", 1),
                new QuestionChunk("q2", "a.md", "A", "Java基础", "中等", "TRUE_FALSE", List.of(), "c2", List.of(), "true", "", "all", 2),
                new QuestionChunk("q3", "b.md", "B", "服务注册与发现", "困难", "SHORT_ANSWER", List.of(), "c3", List.of(), "ans", "", "all", 1)
        ));

        Map<String, Object> summary = service.summary();

        assertThat(summary).containsEntry("totalQuestions", 3);
        assertThat(summary).containsEntry("topicCount", 2);
        assertThat(summary).containsEntry("totalKnowledgePoints", 2);
        assertThat(summary).containsKey("topics");
        assertThat(summary).containsKey("difficultyBreakdown");
        assertThat(summary).containsKey("topicDifficultyBreakdown");
        assertThat(summary).containsKey("questions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) summary.get("questions");
        assertThat(questions).hasSize(3);
        assertThat(questions.get(0))
                .containsEntry("id", "q1")
                .containsEntry("content", "c1")
                .containsEntry("difficulty", "中等")
                .containsEntry("type", "选择题")
                .containsEntry("knowledgePoints", List.of("Java基础"))
                .containsEntry("sourcePath", "a.md");
    }

    @Test
    void returnsEmptySummaryMessageWhenQuestionBankHasNoQuestions() {
        QuestionBankSummaryService service = new QuestionBankSummaryService(List.of());

        Map<String, Object> summary = service.summary();

        assertThat(summary).containsEntry("totalQuestions", 0);
        assertThat(summary).containsEntry("totalKnowledgePoints", 0);
        assertThat(summary).containsEntry("topicCount", 0);
        assertThat(summary).containsEntry("message", "题库暂无可查询数据");
    }
}
