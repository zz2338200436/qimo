package com._202510007517.platform.agent.questionbank;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

    @Test
    void skipsMalformedQuestionBlocksAndKeepsLaterValidQuestions() {
        QuestionBankDocument document = new QuestionBankDocument(
                "doc-2",
                "分布式题库",
                "docs/question-bank/distributed/distributed-basic.md",
                Map.of("topic", "分布式基础", "roleScope", "teacher"),
                """
                ## Question
                difficulty: 中等
                type: SINGLE_CHOICE
                content: CAP 理论中的 C 表示什么？
                options:
                - A. Consistency
                - B. Capacity
                answer: A

                ## Question
                difficulty: 困难
                type: TRUE_FALSE
                content: 分布式事务总是无代价。
                analysis: 缺少 answer，应跳过。

                ## Question
                difficulty: 简单
                type: TRUE_FALSE
                content: 心跳机制常用于节点存活检测。
                answer: true
                analysis: 常见于注册中心和集群管理。
                """
        );
        Logger logger = (Logger) LoggerFactory.getLogger(QuestionMarkdownParser.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            QuestionMarkdownParser parser = new QuestionMarkdownParser();
            List<QuestionChunk> questions = parser.parse(document);

            assertThat(questions).hasSize(2);
            assertThat(questions).extracting(QuestionChunk::questionId)
                    .containsExactly("doc-2#1", "doc-2#3");
            assertThat(questions).extracting(QuestionChunk::content)
                    .containsExactly("CAP 理论中的 C 表示什么？", "心跳机制常用于节点存活检测。");

            assertThat(appender.list)
                    .hasSize(1)
                    .allMatch(event -> event.getLevel() == Level.WARN);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .contains("docs/question-bank/distributed/distributed-basic.md")
                    .contains("question #2")
                    .contains("field 'answer' is required");
        } finally {
            logger.detachAppender(appender);
        }
    }
}
