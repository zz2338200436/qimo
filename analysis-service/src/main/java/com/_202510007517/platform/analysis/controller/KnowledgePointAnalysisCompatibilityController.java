package com._202510007517.platform.analysis.controller;

import com._202510007517.platform.analysis.service.AnalysisQueryService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-points/analysis/teacher")
public class KnowledgePointAnalysisCompatibilityController {

    private final AnalysisQueryService analysisQueryService;

    public KnowledgePointAnalysisCompatibilityController(AnalysisQueryService analysisQueryService) {
        this.analysisQueryService = analysisQueryService;
    }

    @GetMapping({"/course/{courseId}", "/course", "/course/"})
    public ResponseResult<Map<String, Object>> getKnowledgePointAnalysis(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable(required = false) String courseId,
            @RequestParam(required = false, name = "courseId") Long courseIdParam,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String knowledgePointId) {
        Long resolvedCourseId = parseLegacyId(courseId, "课程ID");
        if (resolvedCourseId == null && courseIdParam != null) {
            resolvedCourseId = courseIdParam;
        }
        return ResponseResult.success(
                analysisQueryService.getTeacherKnowledgePointAnalysis(
                        resolveTeacherId(userIdHeader),
                        resolvedCourseId,
                        classId,
                        parseLegacyId(studentId, "学生ID"),
                        parseLegacyId(knowledgePointId, "知识点ID")),
                "获取知识点分析数据成功",
                200);
    }

    private static Long resolveTeacherId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(userIdHeader);
    }

    private static Long parseLegacyId(String value, String label) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new LegacyFilterIdException("无效的" + label);
        }
    }

    private static final class LegacyFilterIdException extends RuntimeException {
        private LegacyFilterIdException(String message) {
            super(message);
        }
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(LegacyFilterIdException.class)
    ResponseResult<Void> handleLegacyFilterIdException(LegacyFilterIdException ex) {
        return ResponseResult.failure(ex.getMessage(), 400);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    ResponseResult<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        if ("缺少教师身份".equals(ex.getMessage())) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        return ResponseResult.failure(ex.getMessage(), 400);
    }
}
