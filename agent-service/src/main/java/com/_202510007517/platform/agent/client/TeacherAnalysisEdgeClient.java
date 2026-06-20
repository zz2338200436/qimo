package com._202510007517.platform.agent.client;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "agentTeacherAnalysisEdgeClient", name = "analysis-service", path = "/api/teacher")
public interface TeacherAnalysisEdgeClient {

    @GetMapping("/dashboard")
    ResponseResult<Map<String, Object>> getTeacherDashboard(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange);

    @GetMapping("/learning-summary")
    ResponseResult<Map<String, Object>> getLearningSummary(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange);

    @GetMapping("/score-trend")
    ResponseResult<List<ScoreTrendDTO>> listScoreTrend(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange);

    @GetMapping("/knowledge-points/mastery/student/{studentId}/course/{courseId}")
    ResponseResult<List<KnowledgeMasteryDTO>> listStudentKnowledgeMastery(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("studentId") Long studentId,
            @PathVariable("courseId") Long courseId);
}
