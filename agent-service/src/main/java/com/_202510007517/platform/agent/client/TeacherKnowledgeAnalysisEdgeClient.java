package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(contextId = "agentTeacherKnowledgeAnalysisEdgeClient", name = "analysis-service",
        path = "/api/knowledge-points/analysis/teacher")
public interface TeacherKnowledgeAnalysisEdgeClient {

    @GetMapping("/course")
    ResponseResult<Map<String, Object>> getKnowledgePointAnalysis(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "knowledgePointId", required = false) Long knowledgePointId);
}
