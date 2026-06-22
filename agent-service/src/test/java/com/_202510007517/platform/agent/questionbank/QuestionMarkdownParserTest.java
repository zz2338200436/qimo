package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionMarkdownParserTest {

    @Test
    void parsesStructuredQuestionsFromMarkdown() {
        QuestionBankDocument document = new QuestionBankDocument(
                "doc-1",
                "Java基础题库",
                "docs/question-bank/java/java-basic.md",
                Map.of("topic", "Java基础", "roleScope", "teacher"),
                """
                ## Question
                difficulty: 中等
                type: SINGLE_CHOICE
                content: Java中用于表示继承的关键字是什么？
                options:
                - A. import
                - B. extends
                - C. implements
                answer: B
                analysis: extends 用于类继承。
                """
        );

        QuestionMarkdownParser parser = new QuestionMarkdownParser();
        List<QuestionChunk> questions = parser.parse(document);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).topic()).isEqualTo("Java基础");
        assertThat(questions.get(0).difficulty()).isEqualTo("中等");
        assertThat(questions.get(0).type()).isEqualTo("SINGLE_CHOICE");
        assertThat(questions.get(0).options()).containsExactly("A. import", "B. extends", "C. implements");
        assertThat(questions.get(0).answer()).isEqualTo("B");
    }
}
