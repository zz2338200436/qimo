package com._202510007517.platform.analysis.controller;

import com._202510007517.platform.analysis.service.AnalysisTriggerCompatibilityService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/analysis")
public class AnalysisTriggerCompatibilityController {

    private final AnalysisTriggerCompatibilityService analysisTriggerCompatibilityService;

    public AnalysisTriggerCompatibilityController(AnalysisTriggerCompatibilityService analysisTriggerCompatibilityService) {
        this.analysisTriggerCompatibilityService = analysisTriggerCompatibilityService;
    }

    @PostMapping("/warnings/trigger")
    public ResponseResult<Void> triggerWarningAnalysis(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        analysisTriggerCompatibilityService.triggerWarningAnalysis(resolveTeacherId(userIdHeader));
        return ResponseResult.success(null, "学情预警分析已启动，请稍后查看结果", 200);
    }

    @PostMapping("/knowledge-points/trigger")
    public ResponseResult<Void> triggerKnowledgePointAnalysis(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        analysisTriggerCompatibilityService.triggerKnowledgePointAnalysis(resolveTeacherId(userIdHeader));
        return ResponseResult.success(null, "知识点分析更新已启动，请稍后查看结果", 200);
    }

    @PostMapping("/student/{studentId}/course/{courseId}/trigger")
    public ResponseResult<Void> triggerStudentAnalysis(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        analysisTriggerCompatibilityService.triggerStudentAnalysis(resolveTeacherId(userIdHeader), studentId, courseId);
        return ResponseResult.success(null, "学生学情分析已启动，请稍后查看结果", 200);
    }

    @PostMapping("/class/{classId}/course/{courseId}/batch-trigger")
    public ResponseResult<Void> triggerClassAnalysis(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long classId,
            @PathVariable Long courseId) {
        int studentCount = analysisTriggerCompatibilityService.triggerClassAnalysis(
                resolveTeacherId(userIdHeader),
                classId,
                courseId);
        return ResponseResult.success(
                null,
                String.format("班级学情分析已启动，将分析 %d 名学生，请稍后查看结果", studentCount),
                200);
    }

    private static Long resolveTeacherId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(userIdHeader);
    }
}
