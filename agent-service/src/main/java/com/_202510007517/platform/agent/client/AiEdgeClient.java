package com._202510007517.platform.agent.client;

import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(contextId = "agentAiEdgeClient", name = "ai-service", path = "/api/ai")
public interface AiEdgeClient {

    @GetMapping("/question-bank/summary")
    ResponseResult<Map<String, Object>> questionBankSummary(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestHeader(CommonTraceConstants.ACTIVE_ROLE_HEADER) String activeRole,
            @RequestHeader(CommonTraceConstants.ROLES_HEADER) String roles);

    @PostMapping("/generate-questions")
    ResponseResult<Map<String, Object>> generateQuestions(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestHeader(CommonTraceConstants.ACTIVE_ROLE_HEADER) String activeRole,
            @RequestHeader(CommonTraceConstants.ROLES_HEADER) String roles,
            @RequestBody GenerateQuestionsRequestDTO request);

    @PostMapping("/generate-exam")
    ResponseResult<Map<String, Object>> generateExam(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestHeader(CommonTraceConstants.ACTIVE_ROLE_HEADER) String activeRole,
            @RequestHeader(CommonTraceConstants.ROLES_HEADER) String roles,
            @RequestBody GenerateExamRequestDTO request);

    @PostMapping("/learning-suggestions")
    ResponseResult<Map<String, Object>> learningSuggestions(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestHeader(CommonTraceConstants.ACTIVE_ROLE_HEADER) String activeRole,
            @RequestHeader(CommonTraceConstants.ROLES_HEADER) String roles,
            @RequestBody LearningSuggestionRequestDTO request);
}
