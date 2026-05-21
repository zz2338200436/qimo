package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.KnowledgePoint;
import java.util.List;
import java.util.Map;

public interface KnowledgePointService {
    
    // 创建知识点
    KnowledgePoint createKnowledgePoint(KnowledgePoint knowledgePoint);
    
    // 更新知识点
    KnowledgePoint updateKnowledgePoint(Long id, KnowledgePoint knowledgePoint);
    
    // 删除知识点
    void deleteKnowledgePoint(Long id);
    
    // 根据ID获取知识点
    KnowledgePoint getKnowledgePointById(Long id);
    
    // 根据课程ID获取知识点列表
    List<Map<String, Object>> getKnowledgePointsByCourseId(Long courseId);

    // 获取教师可见的知识点列表
    List<Map<String, Object>> getKnowledgePointsByTeacherId(Long teacherId);

    // 获取教师在指定课程下可见的知识点列表
    List<Map<String, Object>> getKnowledgePointsByTeacherId(Long teacherId, Long courseId);
    
    // 为作业设置知识点关联
    void setAssignmentKnowledgePoints(Long assignmentId, List<Long> knowledgePointIds);
    
    // 为考试设置知识点关联
    void setExamKnowledgePoints(Long examId, List<Long> knowledgePointIds);
    
    // 获取作业关联的知识点
    List<Map<String, Object>> getKnowledgePointsByAssignmentId(Long assignmentId);
    
    // 获取考试关联的知识点
    List<Map<String, Object>> getKnowledgePointsByExamId(Long examId);
    
    // 分析并更新学生的知识点掌握情况
    void analyzeStudentKnowledgePointMastery(Long studentId, Long courseId);
    
    // 获取学生的知识点掌握情况
    List<Map<String, Object>> getStudentKnowledgeMastery(Long studentId, Long courseId);
    
    // 获取课程知识点掌握情况统计（教师端）
    List<Map<String, Object>> getKnowledgePointMasteryStats(Long courseId);
    
    // 根据知识点ID分析学生掌握情况
    Map<String, Object> analyzeKnowledgePointMasteryForStudent(Long studentId, Long knowledgePointId);
}
