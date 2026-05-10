package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.EarlyWarning;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.mapper.AssignmentSubmissionMapper;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import com._202510007517.major_assignment.service.EarlyWarningService;
import com._202510007517.major_assignment.service.KnowledgePointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简化版的学情预警分析服务实现
 * 避免依赖可能不存在的数据库表
 */
@Service
@Primary
public class EarlyWarningAnalysisServiceSimpleImpl implements EarlyWarningAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(EarlyWarningAnalysisServiceSimpleImpl.class);
    
    @Autowired
    private EarlyWarningService earlyWarningService;
    
    @Autowired
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private KnowledgePointService knowledgePointService;
    
    @Override
    @Transactional
    public void analyzeAndGenerateWarnings() {
        logger.info("开始执行学情预警自动分析（简化版）...");
        
        try {
            // 简化实现：只分析现有数据
            logger.info("学情预警自动分析完成（简化版）");
            
        } catch (Exception e) {
            logger.error("学情预警自动分析失败", e);
        }
    }
    
    @Override
    public List<EarlyWarning> analyzeScoreWarnings(Long studentId, Long courseId) {
        List<EarlyWarning> warnings = new ArrayList<>();
        
        try {
            // 获取学生最近的成绩数据（使用现有的方法）
            List<Map<String, Object>> recentScores = studentMapper.getScores(studentId, null, courseId, "month");
            
            if (recentScores.size() >= 1) {
                // 简单的成绩分析
                double totalScore = 0;
                int count = 0;
                
                for (Map<String, Object> scoreData : recentScores) {
                    Object scoreObj = scoreData.get("score");
                    if (scoreObj != null) {
                        double score = scoreObj instanceof Double ? (Double) scoreObj : Double.parseDouble(scoreObj.toString());
                        totalScore += score;
                        count++;
                    }
                }
                
                if (count > 0) {
                    double averageScore = totalScore / count;
                    
                    // 低分预警（平均分低于60分）
                    if (averageScore < 60) {
                        EarlyWarning warning = createWarning(studentId, courseId, 
                            "成绩偏低", "高", 
                            String.format("学生平均成绩为%.1f，低于及格线", averageScore));
                        warnings.add(warning);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("分析成绩预警失败，学生ID: {}, 课程ID: {}", studentId, courseId, e);
        }
        
        return warnings;
    }
    
    @Override
    public List<EarlyWarning> analyzeAssignmentWarnings(Long studentId, Long courseId) {
        List<EarlyWarning> warnings = new ArrayList<>();
        
        try {
            // 获取学生作业提交统计信息
            Map<String, Object> submissionStats = assignmentSubmissionMapper.getStudentSubmissionStats(studentId, courseId, 30);
            
            if (submissionStats != null) {
                Integer totalAssignments = (Integer) submissionStats.get("totalAssignments");
                Integer submittedAssignments = (Integer) submissionStats.get("submittedAssignments");
                
                if (totalAssignments != null && totalAssignments > 0 && submittedAssignments != null) {
                    double submissionRate = (double) submittedAssignments / totalAssignments;
                    
                    // 作业提交率低预警
                    if (submissionRate < 0.7) {
                        EarlyWarning warning = createWarning(studentId, courseId, 
                            "作业提交率低", "高", 
                            String.format("学生作业提交率为%.1f%%，低于正常水平", submissionRate * 100));
                        warnings.add(warning);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("分析作业预警失败，学生ID: {}, 课程ID: {}", studentId, courseId, e);
        }
        
        return warnings;
    }
    
    @Override
    public List<EarlyWarning> analyzeProgressWarnings(Long studentId, Long courseId) {
        List<EarlyWarning> warnings = new ArrayList<>();
        
        try {
            // 简化的进度分析
            logger.debug("分析学习进度预警，学生ID: {}, 课程ID: {}", studentId, courseId);
            
        } catch (Exception e) {
            logger.error("分析学习进度预警失败，学生ID: {}, 课程ID: {}", studentId, courseId, e);
        }
        
        return warnings;
    }
    
    @Override
    @Transactional
    public void updateKnowledgePointAnalysis() {
        logger.info("开始执行知识点掌握情况自动分析...");
        
        try {
            // 获取所有活跃的学生和课程组合
            List<Map<String, Object>> studentCourses = studentMapper.getAllActiveStudentCourses();
            
            int updatedCount = 0;
            
            for (Map<String, Object> sc : studentCourses) {
                Long studentId = ((Number) sc.get("studentId")).longValue();
                Long courseId = ((Number) sc.get("courseId")).longValue();
                
                // 使用真正的知识点分析服务
                knowledgePointService.analyzeStudentKnowledgePointMastery(studentId, courseId);
                updatedCount++;
            }
            
            logger.info("知识点掌握情况自动分析完成，共更新 {} 个学生的数据", updatedCount);
            
        } catch (Exception e) {
            logger.error("知识点掌握情况自动分析失败", e);
        }
    }
    
    @Override
    public void analyzeStudentKnowledgePoints(Long studentId, Long courseId) {
        try {
            logger.info("分析学生知识点掌握情况，学生ID: {}, 课程ID: {}", studentId, courseId);
            
            // 使用真正的知识点分析服务
            knowledgePointService.analyzeStudentKnowledgePointMastery(studentId, courseId);
            
        } catch (Exception e) {
            logger.error("分析学生知识点掌握情况失败，学生ID: {}, 课程ID: {}", studentId, courseId, e);
        }
    }
    
    @Override
    public void analyzeStudentWarningsRealtime(Long studentId, Long courseId) {
        try {
            logger.info("开始实时分析学生学情预警，学生ID: {}, 课程ID: {}", studentId, courseId);
            
            // 获取教师ID
            Course course = courseMapper.findById(courseId);
            if (course == null) {
                logger.warn("课程不存在，课程ID: {}", courseId);
                return;
            }
            Long teacherId = course.getTeacherId();
            
            // 分析各类预警
            List<EarlyWarning> scoreWarnings = analyzeScoreWarnings(studentId, courseId);
            List<EarlyWarning> assignmentWarnings = analyzeAssignmentWarnings(studentId, courseId);
            List<EarlyWarning> progressWarnings = analyzeProgressWarnings(studentId, courseId);
            
            // 保存预警
            int savedWarnings = 0;
            for (EarlyWarning warning : scoreWarnings) {
                warning.setTeacherId(teacherId);
                if (earlyWarningService.addEarlyWarning(warning)) {
                    savedWarnings++;
                }
            }
            for (EarlyWarning warning : assignmentWarnings) {
                warning.setTeacherId(teacherId);
                if (earlyWarningService.addEarlyWarning(warning)) {
                    savedWarnings++;
                }
            }
            for (EarlyWarning warning : progressWarnings) {
                warning.setTeacherId(teacherId);
                if (earlyWarningService.addEarlyWarning(warning)) {
                    savedWarnings++;
                }
            }
            
            // 分析知识点掌握情况
            analyzeStudentKnowledgePoints(studentId, courseId);
            
            logger.info("实时学情分析完成，学生ID: {}, 课程ID: {}, 生成预警: {} 条", 
                       studentId, courseId, savedWarnings);
            
        } catch (Exception e) {
            logger.error("实时学情分析失败，学生ID: {}, 课程ID: {}", studentId, courseId, e);
        }
    }
    
    @Override
    public void batchAnalyzeStudentsRealtime(List<Long> studentIds, Long courseId) {
        logger.info("开始批量实时分析学生学情，学生数量: {}, 课程ID: {}", studentIds.size(), courseId);
        
        // 使用线程池异步处理，避免阻塞
        for (Long studentId : studentIds) {
            new Thread(() -> {
                analyzeStudentWarningsRealtime(studentId, courseId);
            }).start();
        }
    }
    
    /**
     * 创建预警对象
     */
    private EarlyWarning createWarning(Long studentId, Long courseId, String warningType, 
                                     String warningLevel, String warningMessage) {
        EarlyWarning warning = new EarlyWarning();
        warning.setStudentId(studentId);
        warning.setCourseId(courseId);
        warning.setWarningType(warningType);
        warning.setWarningLevel(warningLevel);
        warning.setWarningMessage(warningMessage);
        warning.setTriggerDate(LocalDateTime.now());
        warning.setIsResolved(false);
        return warning;
    }
}