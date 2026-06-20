package com._202510007517.platform.ai.service;

import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com._202510007517.platform.ai.model.AiModelClient;
import com._202510007517.platform.ai.repository.AiGenerationRecord;
import com._202510007517.platform.ai.repository.AiGenerationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiGenerationService {

    private static final String SUCCESS = "SUCCESS";

    private final AiModelClient modelClient;
    private final AiGenerationRepository generationRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AiGenerationService(AiModelClient modelClient, AiGenerationRepository generationRepository) {
        this(modelClient, generationRepository, new ObjectMapper());
    }

    public AiGenerationService(AiModelClient modelClient,
                               AiGenerationRepository generationRepository,
                               ObjectMapper objectMapper) {
        this.modelClient = modelClient;
        this.generationRepository = generationRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> generateQuestions(Long userId, String userRole, GenerateQuestionsRequestDTO request) {
        return executeAndRecord(userId, userRole, "generate-questions", "generate-questions", request,
                () -> modelClient.generateQuestions(request));
    }

    public Map<String, Object> generateExam(Long userId, String userRole, GenerateExamRequestDTO request) {
        return executeAndRecord(userId, userRole, "generate-exam", "generate-exam", request,
                () -> modelClient.generateExam(request));
    }

    public Map<String, Object> generateLearningSuggestions(Long userId,
                                                           String userRole,
                                                           LearningSuggestionRequestDTO request) {
        Long targetStudentId = resolveTargetStudentId(userId, userRole, request);
        return executeAndRecord(userId, userRole, "learning-suggestions", "learning-suggestions", request,
                () -> modelClient.generateLearningSuggestions(targetStudentId, request));
    }

    private Map<String, Object> executeAndRecord(Long userId,
                                                 String userRole,
                                                 String promptKey,
                                                 String requestType,
                                                 Object request,
                                                 GenerationCall call) {
        long startedAt = System.nanoTime();
        Map<String, Object> response = call.execute();
        long latencyMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        generationRepository.save(new AiGenerationRecord(
                userId,
                normalizeRole(userRole),
                promptKey,
                requestType,
                toJson(request),
                toJson(response),
                modelClient.modelName(requestType),
                SUCCESS,
                null,
                latencyMs));
        return response;
    }

    private Long resolveTargetStudentId(Long userId, String userRole, LearningSuggestionRequestDTO request) {
        if ("STUDENT".equalsIgnoreCase(userRole)) {
            return userId;
        }
        if (request.getStudentId() == null) {
            throw new IllegalArgumentException("缺少必要参数：studentId");
        }
        return request.getStudentId();
    }

    private String normalizeRole(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return "USER";
        }
        return userRole;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("AI 调用记录序列化失败", ex);
        }
    }

    @FunctionalInterface
    private interface GenerationCall {
        Map<String, Object> execute();
    }
}
