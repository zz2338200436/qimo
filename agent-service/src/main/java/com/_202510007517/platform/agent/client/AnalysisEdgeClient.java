package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "agentAnalysisEdgeClient", name = "analysis-service", path = "/api/student")
public interface AnalysisEdgeClient {

    @GetMapping("/stats")
    ResponseResult<Map<String, Object>> getLearningStats(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange);

    @GetMapping("/study-time-distribution")
    ResponseResult<List<Map<String, Object>>> listStudyTimeDistribution(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "type", defaultValue = "daily") String type,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange);
}
