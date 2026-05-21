package com._202510007517.platform.exam.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.service.ExamKnowledgePointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/knowledge-points/exam")
public class ExamKnowledgePointCompatibilityController {

    private final ExamKnowledgePointService examKnowledgePointService;

    public ExamKnowledgePointCompatibilityController(ExamKnowledgePointService examKnowledgePointService) {
        this.examKnowledgePointService = examKnowledgePointService;
    }

    @GetMapping("/{examId}")
    public ResponseResult<List<Map<String, Object>>> listKnowledgePoints(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long examId) {
        return ResponseResult.success(
                examKnowledgePointService.listKnowledgePoints(resolveTeacherId(userIdHeader), examId),
                "获取考试知识点成功",
                200);
    }

    @PostMapping("/{examId}")
    public ResponseResult<Void> replaceKnowledgePoints(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long examId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        examKnowledgePointService.replaceKnowledgePoints(
                resolveTeacherId(userIdHeader),
                examId,
                readKnowledgePointIds(requestBody));
        return ResponseResult.success(null, "设置考试知识点成功", 200);
    }

    private static Long resolveTeacherId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(userIdHeader);
    }

    private static List<Long> readKnowledgePointIds(Map<String, Object> requestBody) {
        if (requestBody == null || requestBody.get("knowledgePointIds") == null) {
            return List.of();
        }
        Object rawIds = requestBody.get("knowledgePointIds");
        if (!(rawIds instanceof List<?> ids)) {
            throw new IllegalArgumentException("knowledgePointIds 必须是数组");
        }
        return ids.stream()
                .map(ExamKnowledgePointCompatibilityController::toLong)
                .toList();
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        throw new IllegalArgumentException("无效的知识点ID");
    }
}
