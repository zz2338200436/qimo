package com._202510007517.platform.analysis.controller;

import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.analysis.service.AnalysisQueryService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentAnalysisCompatibilityController {

    private final AnalysisQueryService analysisQueryService;

    public StudentAnalysisCompatibilityController(AnalysisQueryService analysisQueryService) {
        this.analysisQueryService = analysisQueryService;
    }

    @GetMapping("/stats")
    public ResponseResult<Map<String, Object>> getLearningStats(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange) {
        return ResponseResult.success(
                analysisQueryService.getStudentLearningStats(
                        resolveStudentId(userIdHeader),
                        semester,
                        courseId,
                        timeRange),
                "获取学习统计成功",
                200);
    }

    @GetMapping("/study-time-distribution")
    public ResponseResult<List<Map<String, Object>>> listStudyTimeDistribution(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "type", defaultValue = "daily") String type,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange) {
        return ResponseResult.success(
                analysisQueryService.listStudentStudyTimeDistribution(
                        resolveStudentId(userIdHeader),
                        type,
                        semester,
                        courseId,
                        timeRange),
                "获取学习时间分布成功",
                200);
    }

    @GetMapping("/knowledge-points")
    public ResponseResult<List<Map<String, Object>>> listKnowledgePoints(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange) {
        return ResponseResult.success(
                analysisQueryService.listStudentKnowledgePoints(
                        resolveStudentId(userIdHeader),
                        semester,
                        courseId,
                        timeRange),
                "获取知识点列表成功",
                200);
    }

    @GetMapping("/knowledge-points/{knowledgePointId}")
    public ResponseResult<Map<String, Object>> getKnowledgePointDetail(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long knowledgePointId) {
        return ResponseResult.success(
                analysisQueryService.getStudentKnowledgePointDetail(
                        resolveStudentId(userIdHeader),
                        knowledgePointId),
                "获取知识点详情成功",
                200);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult<Void> handleBadRequest(IllegalArgumentException ex) {
        if ("缺少学生身份".equals(ex.getMessage())) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        return ResponseResult.failure(ex.getMessage(), 400);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseResult<Void> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseResult.failure(ex.getMessage(), ex.getCode());
    }

    private static Long resolveStudentId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少学生身份");
        }
        return Long.valueOf(userIdHeader);
    }
}
