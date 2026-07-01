package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.StudentScoreListItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(contextId = "studentExamEdgeClient", name = "exam-service", path = "/api/student")
public interface StudentExamEdgeClient {

    @GetMapping("/scores")
    ResponseResult<List<StudentScoreListItemDTO>> listScores(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId);
}
