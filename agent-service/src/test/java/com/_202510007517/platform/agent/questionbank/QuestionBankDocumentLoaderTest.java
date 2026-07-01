package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankDocumentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsMarkdownFilesRecursively() throws Exception {
        Path topicDir = Files.createDirectories(tempDir.resolve("java"));
        Files.writeString(topicDir.resolve("java-basic.md"), """
                ---
                title: Java基础题库
                topic: Java基础
                ---
                ## Question
                difficulty: 中等
                type: SINGLE_CHOICE
                content: Java中用于表示继承的关键字是什么？
                answer: extends
                """);

        QuestionBankDocumentLoader loader = new QuestionBankDocumentLoader();
        List<QuestionBankDocument> documents = loader.load(List.of(tempDir.toString()));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("Java基础题库");
    }

    @Test
    void resolvesRelativeDocsPathFromRepositoryRootStyleConfiguration() {
        QuestionBankDocumentLoader loader = new QuestionBankDocumentLoader();

        List<QuestionBankDocument> documents = loader.load(List.of("../docs/question-bank/java/java-basic-sample.md"));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("Java基础中等题库样例");
    }
}
