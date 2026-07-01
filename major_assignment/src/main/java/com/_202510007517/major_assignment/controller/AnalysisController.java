package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学情分析控制器
 * 提供手动触发学情预警和知识点分析的接口
 */
@RestController
@RequestMapping("/api/teacher/analysis")
@RequireLogin(roles = {RoleConstants.TEACHER})
public class AnalysisController extends BaseController {
    
    @Autowired
    private EarlyWarningAnalysisService earlyWarningAnalysisService;
    
    @Autowired
    private StudentMapper studentMapper;
    
    /**
     * 手动触发学情预警分析
     */
    @PostMapping("/warnings/trigger")
    public ResponseResult<Void> triggerWarningAnalysis() {
        try {
            // 异步执行分析任务
            new Thread(() -> {
                earlyWarningAnalysisService.analyzeAndGenerateWarnings();
            }).start();
            
            return ResponseResult.success(null, "学情预警分析已启动，请稍后查看结果", 200);
        } catch (Exception e) {
            return ResponseResult.failure("启动学情预警分析失败", 500);
        }
    }
    
    /**
     * 手动触发知识点分析更新
     */
    @PostMapping("/knowledge-points/trigger")
    public ResponseResult<Void> triggerKnowledgePointAnalysis() {
        try {
            // 异步执行分析任务
            new Thread(() -> {
                earlyWarningAnalysisService.updateKnowledgePointAnalysis();
            }).start();
            
            return ResponseResult.success(null, "知识点分析更新已启动，请稍后查看结果", 200);
        } catch (Exception e) {
            return ResponseResult.failure("启动知识点分析更新失败", 500);
        }
    }
    
    /**
     * 为特定学生和课程触发分析
     */
    @PostMapping("/student/{studentId}/course/{courseId}/trigger")
    public ResponseResult<Void> triggerStudentAnalysis(@PathVariable Long studentId, 
                                                      @PathVariable Long courseId) {
        try {
            // 异步执行学生特定分析
            new Thread(() -> {
                earlyWarningAnalysisService.analyzeStudentWarningsRealtime(studentId, courseId);
            }).start();
            
            return ResponseResult.success(null, "学生学情分析已启动，请稍后查看结果", 200);
        } catch (Exception e) {
            return ResponseResult.failure("启动学生学情分析失败", 500);
        }
    }
    
    /**
     * 批量分析班级学生
     */
    @PostMapping("/class/{classId}/course/{courseId}/batch-trigger")
    public ResponseResult<Void> triggerClassAnalysis(@PathVariable Long classId,
                                                    @PathVariable Long courseId) {
        try {
            // 获取班级所有学生
            List<Map<String, Object>> students = studentMapper.getStudentsByClassId(classId);
            List<Long> studentIds = students.stream()
                .map(s -> (Long) s.get("id"))
                .collect(java.util.stream.Collectors.toList());
            
            // 异步批量分析
            new Thread(() -> {
                earlyWarningAnalysisService.batchAnalyzeStudentsRealtime(studentIds, courseId);
            }).start();
            
            return ResponseResult.success(null, 
                String.format("班级学情分析已启动，将分析 %d 名学生，请稍后查看结果", studentIds.size()), 200);
        } catch (Exception e) {
            return ResponseResult.failure("启动班级学情分析失败", 500);
        }
    }
}
