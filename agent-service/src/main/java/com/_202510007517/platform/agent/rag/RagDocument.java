package com._202510007517.platform.agent.rag;

import java.util.Map;

public record RagDocument(
        String documentId,
        String title,
        String sourcePath,
        Map<String, String> metadata,
        String content
) {
}
