package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.dto.KnowledgePointAnalysisDTO;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.KnowledgePointAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/knowledge-points/analysis/teacher")
public class KnowledgePointAnalysisController extends BaseController {
    
    @Autowired
    private KnowledgePointAnalysisService knowledgePointAnalysisService;
    
    @GetMapping({"/course/{courseId}", "/course", "/course/"})
    public ResponseResult<KnowledgePointAnalysisDTO> getKnowledgePointAnalysis(
            @PathVariable(required = false) String courseId,
            @RequestParam(required = false, name = "courseId") Long courseIdParam,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String knowledgePointId,
            HttpServletRequest requestContext) {
        if (!isLoggedIn(requestContext)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long teacherId = getCurrentUserId(requestContext);
        
        KnowledgePointAnalysisDTO analysis;
        Long finalCourseId = null;
        Long finalStudentId = null;
        Long finalKnowledgePointId = null;
        
        // 处理课程ID
        if (courseId != null) {
            if ("all".equals(courseId)) {
                // 所有课程的情况
                finalCourseId = null;
            } else {
                try {
                    // 单个课程的情况
                    finalCourseId = Long.parseLong(courseId);
                } catch (NumberFormatException e) {
                    // 无效的课程ID格式
                    return ResponseResult.failure("无效的课程ID", 400);
                }
            }
        } else if (courseIdParam != null) {
            // 从查询参数获取课程ID
            finalCourseId = courseIdParam;
        }
        
        // 处理学生ID
        if (studentId != null && !"all".equals(studentId)) {
            try {
                finalStudentId = Long.parseLong(studentId);
            } catch (NumberFormatException e) {
                return ResponseResult.failure("无效的学生ID", 400);
            }
        }
        
        // 处理知识点ID
        if (knowledgePointId != null && !"all".equals(knowledgePointId)) {
            try {
                finalKnowledgePointId = Long.parseLong(knowledgePointId);
            } catch (NumberFormatException e) {
                return ResponseResult.failure("无效的知识点ID", 400);
            }
        }
        
        analysis = knowledgePointAnalysisService.getKnowledgePointAnalysis(finalCourseId, classId, finalStudentId, finalKnowledgePointId, teacherId);
        
        return ResponseResult.success(analysis, "获取知识点分析数据成功", 200);
    }
}
