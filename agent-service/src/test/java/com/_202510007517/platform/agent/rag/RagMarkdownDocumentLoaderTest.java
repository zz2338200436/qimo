package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagMarkdownDocumentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsMarkdownFileWithFrontMatter() throws Exception {
        Path file = tempDir.resolve("guide.md");
        Files.writeString(file, """
                ---
                title: 平台使用指南
                source_type: markdown
                roleScope: teacher
                courseId: 91005
                ---

                # 平台使用指南

                教师可以使用 AI 助手查询教学数据。
                """);

        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        List<RagDocument> documents = loader.load(List.of(file.toString()));

        assertThat(documents).hasSize(1);
        RagDocument document = documents.get(0);
        assertThat(document.documentId()).isEqualTo(file.toAbsolutePath().normalize().toString());
        assertThat(document.title()).isEqualTo("平台使用指南");
        assertThat(document.sourcePath()).isEqualTo(file.toAbsolutePath().normalize().toString());
        assertThat(document.metadata())
                .containsEntry("source_type", "markdown")
                .containsEntry("roleScope", "teacher")
                .containsEntry("courseId", "91005");
        assertThat(document.content()).contains("教师可以使用 AI 助手查询教学数据。");
    }

    @Test
    void loadsAllMarkdownFilesFromDirectoryAndIgnoresMissingPath() throws Exception {
        Path first = tempDir.resolve("first.md");
        Path second = tempDir.resolve("second.txt");
        Files.writeString(first, "# 第一份文档\n\n正文");
        Files.writeString(second, "# 不是 Markdown\n\n正文");

        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        List<RagDocument> documents = loader.load(List.of(tempDir.toString(), tempDir.resolve("missing").toString()));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("第一份文档");
    }
}
