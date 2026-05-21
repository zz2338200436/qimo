package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.KnowledgePoint;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.KnowledgePointService;
import com._202510007517.major_assignment.utils.LogUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/knowledge-points")
public class KnowledgePointController extends BaseController {
    
    private static final Logger logger = LogUtil.getLogger(KnowledgePointController.class);
    
    @Autowired
    private KnowledgePointService knowledgePointService;

    /**
     * 获取教师可见知识点列表。
     * 兼容 teacher-knowledge 页的根列表读取：
     * - 传 courseId 时返回该课程知识点
     * - 不传时返回当前教师全部课程下的知识点
     */
    @GetMapping
    public ResponseResult<List<Map<String, Object>>> getKnowledgePoints(
            @RequestParam(required = false) Long courseId,
            HttpSession session) {
        Map<String, Object> requestParams = courseId != null ? Map.of("courseId", courseId) : null;
        LogUtil.logRequest(logger, "GET", "/api/teacher/knowledge-points", requestParams, getCurrentUserId(session));

        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }

        List<Map<String, Object>> knowledgePoints = courseId != null
                ? knowledgePointService.getKnowledgePointsByTeacherId(getCurrentUserId(session), courseId)
                : knowledgePointService.getKnowledgePointsByTeacherId(getCurrentUserId(session));
        return ResponseResult.success(knowledgePoints, "获取知识点列表成功", 200);
    }
    
    /**
     * 创建知识点
     */
    @PostMapping
    public ResponseResult<KnowledgePoint> createKnowledgePoint(
            @RequestBody KnowledgePoint knowledgePoint,
            HttpSession session) {
        LogUtil.logRequest(logger, "POST", "/api/teacher/knowledge-points", knowledgePoint, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            KnowledgePoint created = knowledgePointService.createKnowledgePoint(knowledgePoint);
            LogUtil.logOperation(logger, "创建知识点", "知识点: " + created.getPointName(), getCurrentUserId(session), true);
            return ResponseResult.success(created, "创建知识点成功", 201);
        } catch (Exception e) {
            LogUtil.logError(logger, "创建知识点失败", e);
            return ResponseResult.failure("创建知识点失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 更新知识点
     */
    @PutMapping("/{id}")
    public ResponseResult<KnowledgePoint> updateKnowledgePoint(
            @PathVariable Long id,
            @RequestBody KnowledgePoint knowledgePoint,
            HttpSession session) {
        LogUtil.logRequest(logger, "PUT", "/api/teacher/knowledge-points/" + id, knowledgePoint, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            KnowledgePoint updated = knowledgePointService.updateKnowledgePoint(id, knowledgePoint);
            LogUtil.logOperation(logger, "更新知识点", "知识点ID: " + id, getCurrentUserId(session), true);
            return ResponseResult.success(updated, "更新知识点成功", 200);
        } catch (Exception e) {
            LogUtil.logError(logger, "更新知识点失败", e);
            return ResponseResult.failure("更新知识点失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 删除知识点
     */
    @DeleteMapping("/{id}")
    public ResponseResult<Void> deleteKnowledgePoint(
            @PathVariable Long id,
            HttpSession session) {
        LogUtil.logRequest(logger, "DELETE", "/api/teacher/knowledge-points/" + id, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            knowledgePointService.deleteKnowledgePoint(id);
            LogUtil.logOperation(logger, "删除知识点", "知识点ID: " + id, getCurrentUserId(session), true);
            return ResponseResult.success(null, "删除知识点成功", 200);
        } catch (Exception e) {
            LogUtil.logError(logger, "删除知识点失败", e);
            return ResponseResult.failure("删除知识点失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 根据ID获取知识点
     */
    @GetMapping("/{id}")
    public ResponseResult<KnowledgePoint> getKnowledgePointById(
            @PathVariable Long id,
            HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/knowledge-points/" + id, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        KnowledgePoint knowledgePoint = knowledgePointService.getKnowledgePointById(id);
        if (knowledgePoint == null) {
            return ResponseResult.failure("知识点不存在", 404);
        }
        return ResponseResult.success(knowledgePoint, "获取知识点成功", 200);
    }
    
    /**
     * 根据课程ID获取知识点列表
     */
    @GetMapping("/course/{courseId}")
    public ResponseResult<List<Map<String, Object>>> getKnowledgePointsByCourse(
            @PathVariable Long courseId,
            HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/knowledge-points/course/" + courseId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<Map<String, Object>> knowledgePoints = knowledgePointService.getKnowledgePointsByCourseId(courseId);
        return ResponseResult.success(knowledgePoints, "获取知识点列表成功", 200);
    }
    
    /**
     * 获取作业关联的知识点
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseResult<List<Map<String, Object>>> getKnowledgePointsByAssignment(
            @PathVariable Long assignmentId,
            HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/knowledge-points/assignment/" + assignmentId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<Map<String, Object>> knowledgePoints = knowledgePointService.getKnowledgePointsByAssignmentId(assignmentId);
        return ResponseResult.success(knowledgePoints, "获取作业知识点成功", 200);
    }
    
    /**
     * 设置作业关联的知识点
     */
    @PostMapping("/assignment/{assignmentId}")
    public ResponseResult<Void> setAssignmentKnowledgePoints(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, Object> requestBody,
            HttpSession session) {
        LogUtil.logRequest(logger, "POST", "/api/teacher/knowledge-points/assignment/" + assignmentId, requestBody, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<Number> knowledgePointIdsRaw = (List<Number>) requestBody.get("knowledgePointIds");
            List<Long> knowledgePointIds = null;
            
            if (knowledgePointIdsRaw != null) {
                knowledgePointIds = knowledgePointIdsRaw.stream()
                    .map(Number::longValue)
                    .toList();
            }
            
            knowledgePointService.setAssignmentKnowledgePoints(assignmentId, knowledgePointIds);
            LogUtil.logOperation(logger, "设置作业知识点", "作业ID: " + assignmentId, getCurrentUserId(session), true);
            return ResponseResult.success(null, "设置作业知识点成功", 200);
        } catch (Exception e) {
            LogUtil.logError(logger, "设置作业知识点失败", e);
            return ResponseResult.failure("设置作业知识点失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 获取考试关联的知识点
     */
    @GetMapping("/exam/{examId}")
    public ResponseResult<List<Map<String, Object>>> getKnowledgePointsByExam(
            @PathVariable Long examId,
            HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/knowledge-points/exam/" + examId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<Map<String, Object>> knowledgePoints = knowledgePointService.getKnowledgePointsByExamId(examId);
        return ResponseResult.success(knowledgePoints, "获取考试知识点成功", 200);
    }
    
    /**
     * 设置考试关联的知识点
     */
    @PostMapping("/exam/{examId}")
    public ResponseResult<Void> setExamKnowledgePoints(
            @PathVariable Long examId,
            @RequestBody Map<String, Object> requestBody,
            HttpSession session) {
        LogUtil.logRequest(logger, "POST", "/api/teacher/knowledge-points/exam/" + examId, requestBody, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<Number> knowledgePointIdsRaw = (List<Number>) requestBody.get("knowledgePointIds");
            List<Long> knowledgePointIds = null;
            
            if (knowledgePointIdsRaw != null) {
                knowledgePointIds = knowledgePointIdsRaw.stream()
                    .map(Number::longValue)
                    .toList();
            }
            
            knowledgePointService.setExamKnowledgePoints(examId, knowledgePointIds);
            LogUtil.logOperation(logger, "设置考试知识点", "考试ID: " + examId, getCurrentUserId(session), true);
            return ResponseResult.success(null, "设置考试知识点成功", 200);
        } catch (Exception e) {
            LogUtil.logError(logger, "设置考试知识点失败", e);
            return ResponseResult.failure("设置考试知识点失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 获取课程知识点掌握情况统计（教师端）
     */
    @GetMapping("/stats/course/{courseId}")
    public ResponseResult<List<Map<String, Object>>> getKnowledgePointMasteryStats(
            @PathVariable Long courseId,
            HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/knowledge-points/stats/course/" + courseId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<Map<String, Object>> stats = knowledgePointService.getKnowledgePointMasteryStats(courseId);
        return ResponseResult.success(stats, "获取知识点掌握统计成功", 200);
    }
    
    /**
     * 获取学生的知识点掌握情况
     */
    @GetMapping("/mastery/student/{studentId}/course/{courseId}")
    public ResponseResult<List<Map<String, Object>>> getStudentKnowledgeMastery(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/knowledge-points/mastery/student/" + studentId + "/course/" + courseId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<Map<String, Object>> mastery = knowledgePointService.getStudentKnowledgeMastery(studentId, courseId);
        return ResponseResult.success(mastery, "获取学生知识点掌握情况成功", 200);
    }
    
    /**
     * 手动触发学生知识点掌握情况分析
     */
    @PostMapping("/analyze/student/{studentId}/course/{courseId}")
    public ResponseResult<Void> analyzeStudentKnowledgeMastery(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            HttpSession session) {
        LogUtil.logRequest(logger, "POST", "/api/teacher/knowledge-points/analyze/student/" + studentId + "/course/" + courseId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            knowledgePointService.analyzeStudentKnowledgePointMastery(studentId, courseId);
            return ResponseResult.success(null, "知识点掌握情况分析完成", 200);
        } catch (Exception e) {
            LogUtil.logError(logger, "知识点掌握情况分析失败", e);
            return ResponseResult.failure("分析失败: " + e.getMessage(), 500);
        }
    }
}
