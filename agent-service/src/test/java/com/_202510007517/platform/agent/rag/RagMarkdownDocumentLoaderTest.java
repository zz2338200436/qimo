package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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

    @Test
    void skipsNullBlankAndMalformedConfiguredPaths() throws Exception {
        Path file = tempDir.resolve("valid.md");
        Files.writeString(file, "# 有效文档\n\n正文");

        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        assertThat(loader.load(null)).isEmpty();

        List<RagDocument> documents = loader.load(Arrays.asList(null, "", "   ", "bad\0path", file.toString()));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("有效文档");
    }

    @Test
    void onlyParsesFrontMatterClosedByExactDelimiterLine() throws Exception {
        Path extraDash = tempDir.resolve("extra-dash.md");
        Files.writeString(extraDash, """
                ---
                title: 不应使用
                ----
                # 正确标题

                正文
                """);

        Path extraText = tempDir.resolve("extra-text.md");
        Files.writeString(extraText, """
                ---
                title: 不应使用
                --- text
                # 文本后缀标题

                正文
                """);

        Path horizontalRule = tempDir.resolve("horizontal-rule.md");
        Files.writeString(horizontalRule, """
                ---
                title: 不应使用

                ---

                # 水平线标题

                正文
                """);

        Path valid = tempDir.resolve("valid.md");
        Files.writeString(valid, """
                ---
                title: 应使用
                ---
                # 错误标题

                正文
                """);

        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        List<RagDocument> documents = loader.load(List.of(
                extraDash.toString(),
                extraText.toString(),
                horizontalRule.toString(),
                valid.toString()
        ));

        assertThat(documents).extracting(RagDocument::title)
                .containsExactly("正确标题", "文本后缀标题", "水平线标题", "应使用");
        assertThat(documents.get(0).metadata()).isEmpty();
        assertThat(documents.get(1).metadata()).isEmpty();
        assertThat(documents.get(2).metadata()).isEmpty();
    }

    @Test
    void stripsMarkdownExtensionCaseInsensitivelyForFallbackTitle() throws Exception {
        Path file = tempDir.resolve("README.MD");
        Files.writeString(file, "正文");

        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        List<RagDocument> documents = loader.load(List.of(file.toString()));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("README");
    }

    @Test
    void resolvesRelativeDocsPathFromRepositoryRootStyleConfiguration() {
        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();

        List<RagDocument> documents = loader.load(List.of("docs/rag/system-platform-knowledge.md"));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("智能学习辅助系统平台知识库");
    }
}
