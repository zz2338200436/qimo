package com._202510007517.platform.agent.questionbank;

import java.util.List;

public record QuestionChunk(
        String questionId,
        String sourcePath,
        String documentTitle,
        String topic,
        String difficulty,
        String type,
        List<String> tags,
        String content,
        List<String> options,
        String answer,
        String analysis,
        String roleScope,
        Integer order
) {
}
