package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(contextId = "agentTeacherWarningEdgeClient", name = "analysis-service", path = "/api/early-warnings/teacher")
public interface TeacherWarningEdgeClient {

    @GetMapping("/stats")
    ResponseResult<Map<String, Object>> getWarningStats(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "courseId", required = false) Long courseId);

    @GetMapping("/list")
    ResponseResult<Map<String, Object>> listWarnings(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "warningType", required = false) String warningType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size);
}
