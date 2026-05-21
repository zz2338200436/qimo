package com._202510007517.platform.analysis.web;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.analysis.service.AnalysisQueryService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class AnalysisQueryController {

    private final AnalysisQueryService analysisQueryService;

    public AnalysisQueryController(AnalysisQueryService analysisQueryService) {
        this.analysisQueryService = analysisQueryService;
    }

    @GetMapping("/dashboard")
    public ResponseResult<Map<String, Object>> getDashboard(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange) {
        return ResponseResult.success(
                analysisQueryService.getTeacherDashboard(
                        resolveTeacherId(userIdHeader),
                        parseFilterId(classId),
                        parseFilterId(courseId),
                        timeRange),
                "获取教师仪表盘数据成功",
                200);
    }

    @GetMapping("/learning-summary")
    public ResponseResult<Map<String, Object>> getLearningSummary(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange) {
        return ResponseResult.success(
                analysisQueryService.getTeacherLearningSummary(
                        resolveTeacherId(userIdHeader),
                        parseFilterId(classId),
                        parseFilterId(courseId),
                        timeRange),
                "获取学生学习汇总数据成功",
                200);
    }

    @GetMapping("/score-trend")
    public ResponseResult<List<ScoreTrendDTO>> listScoreTrend(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange) {
        return ResponseResult.success(
                analysisQueryService.listScoreTrend(
                        resolveTeacherId(userIdHeader),
                        parseFilterId(classId),
                        parseFilterId(courseId),
                        timeRange),
                "获取成绩趋势数据成功",
                200);
    }

    @GetMapping("/knowledge-points/mastery/student/{studentId}/course/{courseId}")
    public ResponseResult<List<KnowledgeMasteryDTO>> listStudentKnowledgeMastery(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        return ResponseResult.success(
                analysisQueryService.listStudentKnowledgeMastery(resolveTeacherId(userIdHeader), studentId, courseId),
                "获取学生知识点掌握情况成功",
                200);
    }

    @GetMapping("/knowledge-points/stats/course/{courseId}")
    public ResponseResult<List<Map<String, Object>>> listKnowledgePointMasteryStats(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long courseId) {
        return ResponseResult.success(
                analysisQueryService.listKnowledgePointMasteryStats(resolveTeacherId(userIdHeader), courseId),
                "获取知识点掌握统计成功",
                200);
    }

    @PostMapping("/knowledge-points/analyze/student/{studentId}/course/{courseId}")
    public ResponseResult<Void> analyzeStudentKnowledgeMastery(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        analysisQueryService.analyzeStudentKnowledgeMastery(resolveTeacherId(userIdHeader), studentId, courseId);
        return ResponseResult.success(null, "知识点掌握情况分析完成", 200);
    }

    private static Long resolveTeacherId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(userIdHeader);
    }

    private static Long parseFilterId(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return Long.valueOf(value);
    }
}
