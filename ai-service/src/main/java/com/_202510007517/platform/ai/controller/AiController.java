package com._202510007517.platform.ai.controller;

import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com._202510007517.platform.ai.service.AiGenerationService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {

    private final AiGenerationService aiGenerationService;

    public AiController(AiGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    @PostMapping("/generate-questions")
    public ResponseResult<Map<String, Object>> generateQuestions(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRole,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String roles,
            @RequestBody @Valid GenerateQuestionsRequestDTO request) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        return ResponseResult.success(
                aiGenerationService.generateQuestions(userId, resolveRole(activeRole, roles), request),
                "生成题目成功",
                200);
    }

    @PostMapping("/generate-exam")
    public ResponseResult<Map<String, Object>> generateExam(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRole,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String roles,
            @RequestBody @Valid GenerateExamRequestDTO request) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        return ResponseResult.success(
                aiGenerationService.generateExam(userId, resolveRole(activeRole, roles), request),
                "生成试卷成功",
                200);
    }

    @PostMapping("/learning-suggestions")
    public ResponseResult<Map<String, Object>> getLearningSuggestions(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRole,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String roles,
            @RequestBody(required = false) LearningSuggestionRequestDTO request) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        LearningSuggestionRequestDTO safeRequest = request == null ? new LearningSuggestionRequestDTO() : request;
        return ResponseResult.success(
                aiGenerationService.generateLearningSuggestions(userId, resolveRole(activeRole, roles), safeRequest),
                "获取学习建议成功",
                200);
    }

    private static Long resolveUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        return Long.valueOf(userIdHeader);
    }

    private static String resolveRole(String activeRole, String roles) {
        if (activeRole != null && !activeRole.isBlank()) {
            return activeRole;
        }
        if (roles == null || roles.isBlank()) {
            return "USER";
        }
        return roles.split(",")[0].trim();
    }
}
