package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagMarkdownChunkerTest {

    @Test
    void splitsDocumentByHeadingsAndPreservesIntroAndMetadata() {
        RagDocument document = new RagDocument(
                "doc-1",
                "RAG 方案",
                "docs/rag-knowledge-base.md",
                Map.of("roleScope", "teacher", "courseId", "91005"),
                """
                # RAG 方案

                总体介绍。

                ## 适合回答的问题

                RAG 适合回答知识解释类问题。

                #### 子主题

                H4 子标题应保留在正文中。

                ### 不适合回答的问题

                RAG 不适合回答实时成绩和删除通知。
                """);

        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).chunkId()).isEqualTo("doc-1#1");
        assertThat(chunks.get(0).documentId()).isEqualTo("doc-1");
        assertThat(chunks.get(0).documentTitle()).isEqualTo("RAG 方案");
        assertThat(chunks.get(0).sectionTitle()).isEqualTo("RAG 方案");
        assertThat(chunks.get(0).sourcePath()).isEqualTo("docs/rag-knowledge-base.md");
        assertThat(chunks.get(0).roleScope()).isEqualTo("teacher");
        assertThat(chunks.get(0).courseId()).isEqualTo("91005");
        assertThat(chunks.get(0).sortOrder()).isEqualTo(1);
        assertThat(chunks.get(0).content()).isEqualTo("总体介绍。");
        assertThat(chunks.get(1).chunkId()).isEqualTo("doc-1#2");
        assertThat(chunks.get(1).sectionTitle()).isEqualTo("适合回答的问题");
        assertThat(chunks.get(1).content()).contains("知识解释类问题", "#### 子主题");
        assertThat(chunks.get(2).chunkId()).isEqualTo("doc-1#3");
        assertThat(chunks.get(2).sectionTitle()).isEqualTo("不适合回答的问题");
        assertThat(chunks.get(2).sortOrder()).isEqualTo(3);
    }

    @Test
    void defaultsRoleScopeToAllAndUsesDocumentTitleWhenNoSectionExists() {
        RagDocument document = new RagDocument("doc-2", "平台指南", "platform.md", Map.of(), "只有一段正文。");

        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).sectionTitle()).isEqualTo("平台指南");
        assertThat(chunks.get(0).roleScope()).isEqualTo("all");
        assertThat(chunks.get(0).content()).isEqualTo("只有一段正文。");
    }

    @Test
    void defaultsRoleScopeToAllWhenMetadataIsNull() {
        RagDocument document = new RagDocument("doc-3", "空元数据指南", "empty.md", null, "正文。");

        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).roleScope()).isEqualTo("all");
        assertThat(chunks.get(0).courseId()).isNull();
    }
}
