package com._202510007517.platform.course.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.service.TeacherKnowledgePointService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/knowledge-points")
public class TeacherKnowledgePointCompatibilityController {

    private final TeacherKnowledgePointService teacherKnowledgePointService;

    public TeacherKnowledgePointCompatibilityController(TeacherKnowledgePointService teacherKnowledgePointService) {
        this.teacherKnowledgePointService = teacherKnowledgePointService;
    }

    @GetMapping
    public ResponseResult<List<Map<String, Object>>> listKnowledgePoints(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        return ResponseResult.success(
                teacherKnowledgePointService.listKnowledgePoints(resolveTeacherId(userIdHeader), courseId),
                "获取知识点列表成功",
                200);
    }

    @GetMapping("/course/{courseId}")
    public ResponseResult<List<Map<String, Object>>> listKnowledgePointsByCourse(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long courseId) {
        return ResponseResult.success(
                teacherKnowledgePointService.listKnowledgePointsByCourse(resolveTeacherId(userIdHeader), courseId),
                "获取知识点列表成功",
                200);
    }

    @GetMapping("/{knowledgePointId}")
    public ResponseResult<Map<String, Object>> getKnowledgePoint(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long knowledgePointId) {
        return ResponseResult.success(
                teacherKnowledgePointService.getKnowledgePoint(resolveTeacherId(userIdHeader), knowledgePointId),
                "获取知识点成功",
                200);
    }

    @PostMapping
    public ResponseResult<Map<String, Object>> createKnowledgePoint(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody Map<String, Object> payload) {
        return ResponseResult.success(
                teacherKnowledgePointService.createKnowledgePoint(resolveTeacherId(userIdHeader), payload),
                "创建知识点成功",
                201);
    }

    @PutMapping("/{knowledgePointId}")
    public ResponseResult<Map<String, Object>> updateKnowledgePoint(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long knowledgePointId,
            @RequestBody Map<String, Object> payload) {
        return ResponseResult.success(
                teacherKnowledgePointService.updateKnowledgePoint(resolveTeacherId(userIdHeader), knowledgePointId, payload),
                "更新知识点成功",
                200);
    }

    @DeleteMapping("/{knowledgePointId}")
    public ResponseResult<Void> deleteKnowledgePoint(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long knowledgePointId) {
        teacherKnowledgePointService.deleteKnowledgePoint(resolveTeacherId(userIdHeader), knowledgePointId);
        return ResponseResult.success(null, "删除知识点成功", 200);
    }

    private static Long resolveTeacherId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(userIdHeader);
    }
}
