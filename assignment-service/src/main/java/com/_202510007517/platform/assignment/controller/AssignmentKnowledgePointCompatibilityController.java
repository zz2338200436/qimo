package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.service.AssignmentKnowledgePointService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
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
@RequestMapping("/api/teacher/knowledge-points/assignment")
public class AssignmentKnowledgePointCompatibilityController {

    private final AssignmentKnowledgePointService assignmentKnowledgePointService;

    public AssignmentKnowledgePointCompatibilityController(AssignmentKnowledgePointService assignmentKnowledgePointService) {
        this.assignmentKnowledgePointService = assignmentKnowledgePointService;
    }

    @GetMapping("/{assignmentId}")
    public ResponseResult<List<Map<String, Object>>> listKnowledgePoints(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long assignmentId) {
        return ResponseResult.success(
                assignmentKnowledgePointService.listKnowledgePoints(resolveTeacherId(userIdHeader), assignmentId),
                "获取作业知识点成功",
                200);
    }

    @PostMapping("/{assignmentId}")
    public ResponseResult<Void> replaceKnowledgePoints(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long assignmentId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        assignmentKnowledgePointService.replaceKnowledgePoints(
                resolveTeacherId(userIdHeader),
                assignmentId,
                readKnowledgePointIds(requestBody));
        return ResponseResult.success(null, "设置作业知识点成功", 200);
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
                .map(AssignmentKnowledgePointCompatibilityController::toLong)
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
