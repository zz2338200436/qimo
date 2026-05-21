package com._202510007517.platform.legacy.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/knowledge-points")
public class LegacyKnowledgeReadController {

    private final LegacyKnowledgeReadService legacyKnowledgeReadService;

    public LegacyKnowledgeReadController(LegacyKnowledgeReadService legacyKnowledgeReadService) {
        this.legacyKnowledgeReadService = legacyKnowledgeReadService;
    }

    @GetMapping
    public ResponseResult<List<Map<String, Object>>> listKnowledgePoints(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String teacherIdHeader,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        Long teacherId = resolveTeacherId(teacherIdHeader);
        return ResponseResult.success(
                legacyKnowledgeReadService.listKnowledgePoints(teacherId, courseId),
                "获取知识点列表成功",
                200);
    }

    private static Long resolveTeacherId(String teacherIdHeader) {
        if (teacherIdHeader == null || teacherIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(teacherIdHeader);
    }
}
