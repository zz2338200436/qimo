package com._202510007517.platform.exam.domain;

public record ExamQuestionRecord(
        Long id,
        Long examId,
        String questionText,
        String questionType,
        String optionsJson,
        String correctAnswer,
        Integer score,
        Long knowledgePointId,
        Integer sortOrder) {
}
