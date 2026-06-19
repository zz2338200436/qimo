package com._202510007517.platform.agent.rag;

public record RagChunk(
        String chunkId,
        String documentId,
        String documentTitle,
        String sectionTitle,
        String sourcePath,
        String roleScope,
        String courseId,
        String content,
        int sortOrder
) {
}
