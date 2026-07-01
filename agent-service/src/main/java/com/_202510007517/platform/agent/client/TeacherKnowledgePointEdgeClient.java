package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "agentTeacherKnowledgePointEdgeClient", name = "course-service", path = "/api/teacher/knowledge-points")
public interface TeacherKnowledgePointEdgeClient {

    @GetMapping
    ResponseResult<List<Map<String, Object>>> listKnowledgePoints(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "courseId", required = false) Long courseId);
}
