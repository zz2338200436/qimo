package com._202510007517.platform.ai.service;

import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com._202510007517.platform.ai.model.AiModelClient;
import com._202510007517.platform.ai.repository.AiGenerationRecord;
import com._202510007517.platform.ai.repository.AiGenerationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiGenerationServiceTest {

    @Test
    void generateQuestionsDelegatesToModelClientAndRecordsHistory() {
        GenerateQuestionsRequestDTO request = new GenerateQuestionsRequestDTO();
        request.setTopic("Java基础");
        request.setCount(2);
        request.setDifficulty("中等");
        CapturingRepository repository = new CapturingRepository();
        AiGenerationService service = new AiGenerationService(new StubModelClient(), repository);

        Map<String, Object> result = service.generateQuestions(7L, "TEACHER", request);

        assertThat(result).containsEntry("topic", "Java基础");
        assertThat(result).containsEntry("count", 2);
        assertThat(result).containsEntry("difficulty", "中等");
        assertThat((List<?>) result.get("questions")).hasSize(2);
        assertThat(repository.saved).isNotNull();
        assertThat(repository.saved.userId()).isEqualTo(7L);
        assertThat(repository.saved.userRole()).isEqualTo("TEACHER");
        assertThat(repository.saved.promptKey()).isEqualTo("generate-questions");
        assertThat(repository.saved.requestType()).isEqualTo("generate-questions");
        assertThat(repository.saved.modelName()).isEqualTo("stub-question-bank");
        assertThat(repository.saved.status()).isEqualTo("SUCCESS");
        assertThat(repository.saved.latencyMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void learningSuggestionsKeepModelClientDefaultNameInHistory() {
        LearningSuggestionRequestDTO request = new LearningSuggestionRequestDTO();
        request.setStudentId(42L);
        CapturingRepository repository = new CapturingRepository();
        AiGenerationService service = new AiGenerationService(new StubModelClient(), repository);

        service.generateLearningSuggestions(7L, "TEACHER", request);

        assertThat(repository.saved).isNotNull();
        assertThat(repository.saved.requestType()).isEqualTo("learning-suggestions");
        assertThat(repository.saved.modelName()).isEqualTo("stub-local-model");
    }

    @Test
    void generateQuestionsStillReturnsModelResultWhenHistorySaveFails() {
        GenerateQuestionsRequestDTO request = new GenerateQuestionsRequestDTO();
        request.setTopic("Java基础");
        request.setCount(2);
        request.setDifficulty("中等");
        AiGenerationService service = new AiGenerationService(new StubModelClient(), new FailingRepository());

        Map<String, Object> result = service.generateQuestions(7L, "TEACHER", request);

        assertThat(result).containsEntry("topic", "Java基础");
        assertThat(result).containsEntry("count", 2);
        assertThat((List<?>) result.get("questions")).hasSize(2);
    }

    private static final class StubModelClient implements AiModelClient {

        @Override
        public Map<String, Object> generateQuestions(GenerateQuestionsRequestDTO request) {
            return Map.of(
                    "topic", request.getTopic(),
                    "count", request.getCount(),
                    "difficulty", request.getDifficulty(),
                    "questions", List.of(
                            Map.of("id", 1, "content", "Java基础相关题目 1"),
                            Map.of("id", 2, "content", "Java基础相关题目 2")));
        }

        @Override
        public Map<String, Object> generateLearningSuggestions(Long targetStudentId,
                                                               LearningSuggestionRequestDTO request) {
            return Map.of("studentId", targetStudentId, "suggestions", List.of("建议复习 Java 基础"));
        }

        @Override
        public String modelName(String requestType) {
            if ("generate-questions".equals(requestType)) {
                return "stub-question-bank";
            }
            return modelName();
        }

        @Override
        public String modelName() {
            return "stub-local-model";
        }
    }

    private static final class CapturingRepository implements AiGenerationRepository {

        private AiGenerationRecord saved;

        @Override
        public void save(AiGenerationRecord record) {
            this.saved = record;
        }
    }

    private static final class FailingRepository implements AiGenerationRepository {

        @Override
        public void save(AiGenerationRecord record) {
            throw new IllegalStateException("db down");
        }
    }
}
