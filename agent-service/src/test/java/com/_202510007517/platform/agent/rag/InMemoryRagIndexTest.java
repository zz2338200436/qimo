package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRagIndexTest {

    @Test
    void ranksChunksByCosineSimilarityAndFiltersByRoleScope() {
        RagChunk teacherChunk = chunk("c1", "teacher", "服务注册与发现说明");
        RagChunk studentChunk = chunk("c2", "student", "学生复习路径说明");
        RagChunk allChunk = chunk("c3", "all", "平台通用说明");
        InMemoryRagIndex index = new InMemoryRagIndex();
        index.replaceAll(List.of(
                new InMemoryRagIndex.IndexedChunk(teacherChunk, List.of(1.0, 0.0)),
                new InMemoryRagIndex.IndexedChunk(studentChunk, List.of(0.0, 1.0)),
                new InMemoryRagIndex.IndexedChunk(allChunk, List.of(0.8, 0.2))
        ));

        List<RagSearchResult> results = index.search(List.of(1.0, 0.0), "TEACHER", 2, 0.0);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).chunk().chunkId()).isEqualTo("c1");
        assertThat(results.get(1).chunk().chunkId()).isEqualTo("c3");
        assertThat(results).extracting(result -> result.chunk().chunkId()).doesNotContain("c2");
    }

    @Test
    void appliesMinimumScore() {
        InMemoryRagIndex index = new InMemoryRagIndex();
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(chunk("c1", "all", "低相关"), List.of(0.0, 1.0))));

        List<RagSearchResult> results = index.search(List.of(1.0, 0.0), "STUDENT", 4, 0.1);

        assertThat(results).isEmpty();
    }

    private RagChunk chunk(String id, String roleScope, String content) {
        return new RagChunk(id, "doc", "文档", "章节", "source.md", roleScope, null, content, 1);
    }
}
