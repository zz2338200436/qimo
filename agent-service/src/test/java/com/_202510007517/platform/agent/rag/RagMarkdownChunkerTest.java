package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagMarkdownChunkerTest {

    @Test
    void splitsDocumentByHeadingsAndPreservesMetadata() {
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

                ## 不适合回答的问题

                RAG 不适合回答实时成绩和删除通知。
                """);

        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).chunkId()).isEqualTo("doc-1#1");
        assertThat(chunks.get(0).documentTitle()).isEqualTo("RAG 方案");
        assertThat(chunks.get(0).sectionTitle()).isEqualTo("适合回答的问题");
        assertThat(chunks.get(0).roleScope()).isEqualTo("teacher");
        assertThat(chunks.get(0).courseId()).isEqualTo("91005");
        assertThat(chunks.get(0).content()).contains("知识解释类问题");
        assertThat(chunks.get(1).sectionTitle()).isEqualTo("不适合回答的问题");
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
}
