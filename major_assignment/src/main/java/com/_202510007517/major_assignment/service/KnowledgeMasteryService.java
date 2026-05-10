package com._202510007517.major_assignment.service;

/**
 * 知识点掌握情况服务接口
 */
public interface KnowledgeMasteryService {
    
    /**
     * 根据作业提交更新知识点掌握情况
     * @param submissionId 作业提交ID
     * @param studentId 学生ID
     * @param assignmentId 作业ID
     * @param totalScore 作业总分
     */
    void updateMasteryByAssignment(Long submissionId, Long studentId, Long assignmentId, Integer totalScore);
    
    /**
     * 根据考试提交更新知识点掌握情况
     * @param submissionId 考试提交ID
     * @param studentId 学生ID
     * @param examId 考试ID
     * @param totalScore 考试总分
     */
    void updateMasteryByExam(Long submissionId, Long studentId, Long examId, Integer totalScore);
    
    /**
     * 根据题目得分更新知识点掌握情况
     * @param studentId 学生ID
     * @param knowledgePointId 知识点ID
     * @param earnedScore 得分
     * @param totalScore 总分
     */
    void updateMasteryByQuestion(Long studentId, Long knowledgePointId, Integer earnedScore, Integer totalScore);
}

