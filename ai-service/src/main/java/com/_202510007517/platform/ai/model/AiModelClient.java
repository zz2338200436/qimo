package com._202510007517.platform.ai.model;

import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;

import java.util.Map;

public interface AiModelClient {

    default Map<String, Object> generateQuestions(GenerateQuestionsRequestDTO request) {
        throw new UnsupportedOperationException("generateQuestions is not implemented");
    }

    default Map<String, Object> generateExam(GenerateExamRequestDTO request) {
        throw new UnsupportedOperationException("generateExam is not implemented");
    }

    default Map<String, Object> generateLearningSuggestions(Long targetStudentId,
                                                            LearningSuggestionRequestDTO request) {
        throw new UnsupportedOperationException("generateLearningSuggestions is not implemented");
    }

    default String modelName() {
        return "local-mock-model";
    }
}
