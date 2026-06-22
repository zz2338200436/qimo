package com._202510007517.platform.agent.questionbank;

import java.util.Map;

public record QuestionBankDocument(
        String documentId,
        String title,
        String sourcePath,
        Map<String, String> metadata,
        String content
) {
}
