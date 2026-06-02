package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.dto.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/frontend")
public class FrontendCapabilityController {

    @GetMapping("/capabilities")
    public ResponseResult<Map<String, Object>> getCapabilities() {
        Map<String, Object> studentCapabilities = new LinkedHashMap<>();
        studentCapabilities.put("dashboardPerformance", false);
        studentCapabilities.put("courses", true);
        studentCapabilities.put("courseDetail", true);
        studentCapabilities.put("assignments", true);
        studentCapabilities.put("assignmentDetail", true);
        studentCapabilities.put("assignmentSubmit", true);
        studentCapabilities.put("exams", true);
        studentCapabilities.put("examDetail", true);
        studentCapabilities.put("examSubmit", true);
        studentCapabilities.put("scores", true);
        studentCapabilities.put("stats", true);
        studentCapabilities.put("studyTimeDistribution", true);
        studentCapabilities.put("knowledgePoints", true);
        studentCapabilities.put("studentProfile", true);
        studentCapabilities.put("studentProfileUpdate", true);
        studentCapabilities.put("changePassword", true);
        studentCapabilities.put("notificationSettings", false);
        studentCapabilities.put("privacySettings", false);
        studentCapabilities.put("exportData", true);
        studentCapabilities.put("avatarUpload", true);
        studentCapabilities.put("activities", false);

        Map<String, Object> teacherCapabilities = new LinkedHashMap<>();
        teacherCapabilities.put("notificationPersistence", false);
        teacherCapabilities.put("knowledgeAnalysisExport", false);
        teacherCapabilities.put("aiToolExport", false);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("student", studentCapabilities);
        payload.put("teacher", teacherCapabilities);

        return ResponseResult.success(payload, "获取前端能力矩阵成功", 200);
    }
}
