package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.KnowledgePoint;
import com._202510007517.major_assignment.mapper.KnowledgePointMapper;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.KnowledgePointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgePointServiceImpl implements KnowledgePointService {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgePointServiceImpl.class);
    
    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Autowired
    private CourseService courseService;
    
    @Override
    @Transactional
    public KnowledgePoint createKnowledgePoint(KnowledgePoint knowledgePoint) {
        knowledgePointMapper.insertKnowledgePoint(knowledgePoint);
        logger.info("创建知识点成功: {}", knowledgePoint.getPointName());
        return knowledgePoint;
    }
    
    @Override
    @Transactional
    public KnowledgePoint updateKnowledgePoint(Long id, KnowledgePoint knowledgePoint) {
        knowledgePoint.setId(id);
        knowledgePointMapper.updateKnowledgePoint(knowledgePoint);
        logger.info("更新知识点成功: {}", id);
        return knowledgePoint;
    }
    
    @Override
    @Transactional
    public void deleteKnowledgePoint(Long id) {
        knowledgePointMapper.deleteKnowledgePoint(id);
        logger.info("删除知识点成功: {}", id);
    }
    
    @Override
    public KnowledgePoint getKnowledgePointById(Long id) {
        return knowledgePointMapper.findById(id);
    }
    
    @Override
    public List<Map<String, Object>> getKnowledgePointsByCourseId(Long courseId) {
        return knowledgePointMapper.getKnowledgePointsByCourseId(courseId);
    }

    @Override
    public List<Map<String, Object>> getKnowledgePointsByTeacherId(Long teacherId) {
        List<Long> courseIds = courseService.findCourseIdsByTeacherId(teacherId);
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }

        return knowledgePointMapper.findByCourseIds(courseIds)
                .stream()
                .map(point -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", point.getId());
                    item.put("pointName", point.getPointName());
                    item.put("description", point.getDescription());
                    item.put("difficulty", point.getDifficulty());
                    item.put("orderIndex", point.getOrderIndex());
                    item.put("courseId", point.getCourseId());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getKnowledgePointsByTeacherId(Long teacherId, Long courseId) {
        if (courseId == null) {
            return getKnowledgePointsByTeacherId(teacherId);
        }

        List<Long> courseIds = courseService.findCourseIdsByTeacherId(teacherId);
        if (courseIds == null || !courseIds.contains(courseId)) {
            return List.of();
        }

        return knowledgePointMapper.getKnowledgePointsByCourseId(courseId);
    }
    
    @Override
    @Transactional
    public void setAssignmentKnowledgePoints(Long assignmentId, List<Long> knowledgePointIds) {
        // 先删除现有关联
        knowledgePointMapper.deleteAssignmentKnowledgePoints(assignmentId);
        
        // 添加新关联
        if (knowledgePointIds != null && !knowledgePointIds.isEmpty()) {
            for (Long knowledgePointId : knowledgePointIds) {
                knowledgePointMapper.addAssignmentKnowledgePoint(assignmentId, knowledgePointId);
            }
            logger.info("为作业 {} 设置了 {} 个知识点关联", assignmentId, knowledgePointIds.size());
        }
    }
    
    @Override
    @Transactional
    public void setExamKnowledgePoints(Long examId, List<Long> knowledgePointIds) {
        // 先删除现有关联
        knowledgePointMapper.deleteExamKnowledgePoints(examId);
        
        // 添加新关联
        if (knowledgePointIds != null && !knowledgePointIds.isEmpty()) {
            for (Long knowledgePointId : knowledgePointIds) {
                knowledgePointMapper.addExamKnowledgePoint(examId, knowledgePointId);
            }
            logger.info("为考试 {} 设置了 {} 个知识点关联", examId, knowledgePointIds.size());
        }
    }
    
    @Override
    public List<Map<String, Object>> getKnowledgePointsByAssignmentId(Long assignmentId) {
        return knowledgePointMapper.getKnowledgePointsByAssignmentId(assignmentId);
    }
    
    @Override
    public List<Map<String, Object>> getKnowledgePointsByExamId(Long examId) {
        return knowledgePointMapper.getKnowledgePointsByExamId(examId);
    }
    
    @Override
    @Transactional
    public void analyzeStudentKnowledgePointMastery(Long studentId, Long courseId) {
        logger.info("开始分析学生 {} 在课程 {} 的知识点掌握情况", studentId, courseId);
        
        // 获取课程的所有知识点
        List<Map<String, Object>> knowledgePoints = knowledgePointMapper.getKnowledgePointsByCourseId(courseId);
        
        for (Map<String, Object> kp : knowledgePoints) {
            Long knowledgePointId = ((Number) kp.get("id")).longValue();
            
            // 分析该知识点的掌握情况
            Map<String, Object> masteryResult = analyzeKnowledgePointMasteryForStudent(studentId, knowledgePointId);
            
            if (masteryResult != null && masteryResult.get("masteryLevel") != null) {
                String masteryLevel = (String) masteryResult.get("masteryLevel");
                
                // 更新知识点掌握情况
                knowledgePointMapper.upsertKnowledgeMastery(studentId, knowledgePointId, masteryLevel);
                
                logger.debug("更新学生 {} 知识点 {} 掌握情况: {}", studentId, knowledgePointId, masteryLevel);
            }
        }
        
        logger.info("学生 {} 在课程 {} 的知识点掌握情况分析完成", studentId, courseId);
    }
    
    @Override
    public List<Map<String, Object>> getStudentKnowledgeMastery(Long studentId, Long courseId) {
        return knowledgePointMapper.getStudentKnowledgeMastery(studentId, courseId);
    }
    
    @Override
    public List<Map<String, Object>> getKnowledgePointMasteryStats(Long courseId) {
        return knowledgePointMapper.getKnowledgePointMasteryStats(courseId);
    }
    
    @Override
    public Map<String, Object> analyzeKnowledgePointMasteryForStudent(Long studentId, Long knowledgePointId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取该知识点关联的作业成绩
        List<Map<String, Object>> assignmentScores = knowledgePointMapper.getAssignmentScoresByKnowledgePointAndStudent(knowledgePointId, studentId);
        
        // 获取该知识点关联的考试成绩
        List<Map<String, Object>> examScores = knowledgePointMapper.getExamScoresByKnowledgePointAndStudent(knowledgePointId, studentId);
        
        // 计算综合得分率
        double totalWeightedScore = 0;
        double totalMaxScore = 0;
        int practiceCount = 0;
        
        // 处理作业成绩
        for (Map<String, Object> score : assignmentScores) {
            Object scoreObj = score.get("score");
            Object maxScoreObj = score.get("maxScore");
            
            if (scoreObj != null && maxScoreObj != null) {
                double scoreValue = ((Number) scoreObj).doubleValue();
                double maxScoreValue = ((Number) maxScoreObj).doubleValue();
                
                if (maxScoreValue > 0) {
                    totalWeightedScore += scoreValue;
                    totalMaxScore += maxScoreValue;
                    practiceCount++;
                }
            }
        }
        
        // 处理考试成绩（考试权重更高）
        for (Map<String, Object> score : examScores) {
            Object scoreObj = score.get("score");
            Object maxScoreObj = score.get("maxScore");
            
            if (scoreObj != null && maxScoreObj != null) {
                double scoreValue = ((Number) scoreObj).doubleValue();
                double maxScoreValue = ((Number) maxScoreObj).doubleValue();
                
                if (maxScoreValue > 0) {
                    // 考试权重为1.5倍
                    totalWeightedScore += scoreValue * 1.5;
                    totalMaxScore += maxScoreValue * 1.5;
                    practiceCount++;
                }
            }
        }
        
        // 计算掌握等级
        String masteryLevel = null;
        double scoreRate = 0;
        
        if (totalMaxScore > 0) {
            scoreRate = (totalWeightedScore / totalMaxScore) * 100;
            
            if (scoreRate >= 90) {
                masteryLevel = "优秀";
            } else if (scoreRate >= 75) {
                masteryLevel = "良好";
            } else if (scoreRate >= 60) {
                masteryLevel = "一般";
            } else {
                masteryLevel = "较差";
            }
        }
        
        result.put("knowledgePointId", knowledgePointId);
        result.put("masteryLevel", masteryLevel);
        result.put("scoreRate", scoreRate);
        result.put("practiceCount", practiceCount);
        result.put("assignmentCount", assignmentScores.size());
        result.put("examCount", examScores.size());
        
        return result;
    }
}
