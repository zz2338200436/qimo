package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.mapper.KnowledgeMasteryMapper;
import com._202510007517.major_assignment.service.KnowledgeMasteryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class KnowledgeMasteryServiceImpl implements KnowledgeMasteryService {
    
    @Autowired
    private KnowledgeMasteryMapper knowledgeMasteryMapper;
    
    @Override
    @Transactional
    public void updateMasteryByAssignment(Long submissionId, Long studentId, Long assignmentId, Integer totalScore) {
        try {
            List<Map<String, Object>> questions = knowledgeMasteryMapper.findQuestionsByAssignment(submissionId, assignmentId);
            
            if (questions == null || questions.isEmpty()) {
                Map<String, Object> submission = knowledgeMasteryMapper.getAssignmentSubmissionScore(submissionId);
                if (submission != null && submission.get("score") != null) {
                    Integer score = ((Number) submission.get("score")).intValue();
                    updateMasteryByOverallScore(studentId, assignmentId, score, totalScore, true);
                }
                return;
            }
            
            Map<Long, List<Map<String, Object>>> knowledgePointGroups = new java.util.HashMap<>();
            for (Map<String, Object> question : questions) {
                Long knowledgePointId = ((Number) question.get("knowledge_point_id")).longValue();
                knowledgePointGroups.computeIfAbsent(knowledgePointId, k -> new java.util.ArrayList<>()).add(question);
            }
            
            for (Map.Entry<Long, List<Map<String, Object>>> entry : knowledgePointGroups.entrySet()) {
                Long knowledgePointId = entry.getKey();
                List<Map<String, Object>> questionList = entry.getValue();
                
                double totalEarned = 0;
                double totalPossible = 0;
                
                for (Map<String, Object> question : questionList) {
                    Integer earnedScore = question.get("earned_score") != null ? ((Number) question.get("earned_score")).intValue() : 0;
                    Integer questionTotalScore = question.get("total_score") != null ? ((Number) question.get("total_score")).intValue() : 0;
                    totalEarned += earnedScore;
                    totalPossible += questionTotalScore;
                }
                
                if (totalPossible > 0) {
                    double masteryRate = (totalEarned / totalPossible) * 100;
                    updateMastery(studentId, knowledgePointId, masteryRate);
                }
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    @Override
    @Transactional
    public void updateMasteryByExam(Long submissionId, Long studentId, Long examId, Integer totalScore) {
        try {
            List<Map<String, Object>> questions = knowledgeMasteryMapper.findQuestionsByExam(submissionId, examId);
            
            if (questions == null || questions.isEmpty()) {
                Map<String, Object> submission = knowledgeMasteryMapper.getExamSubmissionScore(submissionId);
                if (submission != null && submission.get("score") != null) {
                    Integer score = ((Number) submission.get("score")).intValue();
                    updateMasteryByOverallScore(studentId, examId, score, totalScore, false);
                }
                return;
            }
            
            Map<Long, List<Map<String, Object>>> knowledgePointGroups = new java.util.HashMap<>();
            for (Map<String, Object> question : questions) {
                Long knowledgePointId = ((Number) question.get("knowledge_point_id")).longValue();
                knowledgePointGroups.computeIfAbsent(knowledgePointId, k -> new java.util.ArrayList<>()).add(question);
            }
            
            for (Map.Entry<Long, List<Map<String, Object>>> entry : knowledgePointGroups.entrySet()) {
                Long knowledgePointId = entry.getKey();
                List<Map<String, Object>> questionList = entry.getValue();
                
                double totalEarned = 0;
                double totalPossible = 0;
                
                for (Map<String, Object> question : questionList) {
                    Integer earnedScore = question.get("earned_score") != null ? ((Number) question.get("earned_score")).intValue() : 0;
                    Integer questionTotalScore = question.get("total_score") != null ? ((Number) question.get("total_score")).intValue() : 0;
                    totalEarned += earnedScore;
                    totalPossible += questionTotalScore;
                }
                
                if (totalPossible > 0) {
                    double masteryRate = (totalEarned / totalPossible) * 100;
                    updateMastery(studentId, knowledgePointId, masteryRate);
                }
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    @Override
    @Transactional
    public void updateMasteryByQuestion(Long studentId, Long knowledgePointId, Integer earnedScore, Integer totalScore) {
        if (totalScore == null || totalScore <= 0) {
            return;
        }
        double masteryRate = (earnedScore.doubleValue() / totalScore.doubleValue()) * 100;
        updateMastery(studentId, knowledgePointId, masteryRate);
    }
    
    private void updateMasteryByOverallScore(Long studentId, Long assessmentId, Integer earnedScore, Integer totalScore, boolean isAssignment) {
        try {
            List<Map<String, Object>> knowledgePoints;
            if (isAssignment) {
                knowledgePoints = knowledgeMasteryMapper.getKnowledgePointsByAssignment(assessmentId);
            } else {
                knowledgePoints = knowledgeMasteryMapper.getKnowledgePointsByExam(assessmentId);
            }
            
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                Map<String, Object> course;
                if (isAssignment) {
                    course = knowledgeMasteryMapper.getCourseByAssignment(assessmentId);
                } else {
                    course = knowledgeMasteryMapper.getCourseByExam(assessmentId);
                }
                
                if (course == null || course.get("course_id") == null) {
                    return;
                }
                
                Long courseId = ((Number) course.get("course_id")).longValue();
                knowledgePoints = knowledgeMasteryMapper.getKnowledgePointsByCourse(courseId);
            }
            
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                return;
            }
            
            double masteryRate = totalScore > 0 ? (earnedScore.doubleValue() / totalScore.doubleValue()) * 100 : 0;
            
            for (Map<String, Object> kp : knowledgePoints) {
                Long knowledgePointId = ((Number) kp.get("id")).longValue();
                updateMastery(studentId, knowledgePointId, masteryRate);
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    private void updateMastery(Long studentId, Long knowledgePointId, double masteryRate) {
        String masteryLevel;
        if (masteryRate >= 85) {
            masteryLevel = "优秀";
        } else if (masteryRate >= 70) {
            masteryLevel = "良好";
        } else if (masteryRate >= 60) {
            masteryLevel = "一般";
        } else {
            masteryLevel = "较差";
        }
        knowledgeMasteryMapper.insertOrUpdate(studentId, knowledgePointId, masteryLevel, null);
    }
}
