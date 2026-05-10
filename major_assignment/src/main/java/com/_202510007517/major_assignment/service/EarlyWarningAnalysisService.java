package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.EarlyWarning;
import java.util.List;

/**
 * 学情预警分析服务接口
 */
public interface EarlyWarningAnalysisService {
    
    /**
     * 自动分析并生成学情预警
     */
    void analyzeAndGenerateWarnings();
    
    /**
     * 分析学生成绩预警
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 生成的预警列表
     */
    List<EarlyWarning> analyzeScoreWarnings(Long studentId, Long courseId);
    
    /**
     * 分析学生作业提交预警
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 生成的预警列表
     */
    List<EarlyWarning> analyzeAssignmentWarnings(Long studentId, Long courseId);
    
    /**
     * 分析学生学习进度预警
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 生成的预警列表
     */
    List<EarlyWarning> analyzeProgressWarnings(Long studentId, Long courseId);
    
    /**
     * 更新知识点掌握情况分析
     */
    void updateKnowledgePointAnalysis();
    
    /**
     * 分析单个学生的知识点掌握情况
     * @param studentId 学生ID
     * @param courseId 课程ID
     */
    void analyzeStudentKnowledgePoints(Long studentId, Long courseId);
    
    /**
     * 实时分析特定学生的学情预警
     * @param studentId 学生ID
     * @param courseId 课程ID
     */
    void analyzeStudentWarningsRealtime(Long studentId, Long courseId);
    
    /**
     * 批量实时分析多个学生的学情
     * @param studentIds 学生ID列表
     * @param courseId 课程ID
     */
    void batchAnalyzeStudentsRealtime(List<Long> studentIds, Long courseId);
}