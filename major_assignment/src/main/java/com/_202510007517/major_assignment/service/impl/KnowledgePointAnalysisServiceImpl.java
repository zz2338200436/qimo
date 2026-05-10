package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.KnowledgePoint;
import com._202510007517.major_assignment.entity.dto.KnowledgePointAnalysisDTO;
import com._202510007517.major_assignment.mapper.KnowledgePointMapper;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.KnowledgePointAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgePointAnalysisServiceImpl implements KnowledgePointAnalysisService {
    
    @Autowired
    private KnowledgePointMapper knowledgePointMapper;
    
    @Autowired
    private CourseService courseService;
    
    @Override
    public KnowledgePointAnalysisDTO getKnowledgePointAnalysis(Long courseId, Long classId, Long studentId, Long knowledgePointId, Long teacherId) {
        KnowledgePointAnalysisDTO analysis = new KnowledgePointAnalysisDTO();
        
        // 处理课程名称
        String courseName = "所有课程";
        boolean isCourseValid = true;
        
        // 获取知识点列表
        List<KnowledgePoint> knowledgePoints;
        if (courseId != null) {
            // 检查课程是否存在且属于当前教师
            Course course = courseService.findById(courseId);
            if (course != null && course.getTeacherId().equals(teacherId)) {
                courseName = course.getCourseName();
                knowledgePoints = knowledgePointMapper.findByCourseId(courseId);
            } else {
                knowledgePoints = new ArrayList<>();
                isCourseValid = false;
            }
        } else {
            // 获取当前教师教授的所有课程ID
            List<Long> courseIds = courseService.findCourseIdsByTeacherId(teacherId);
            if (courseIds == null || courseIds.isEmpty()) {
                knowledgePoints = new ArrayList<>();
            } else {
                knowledgePoints = knowledgePointMapper.findByCourseIds(courseIds);
            }
        }
        
        // 如果指定了知识点ID，过滤知识点列表
        if (knowledgePointId != null) {
            knowledgePoints = knowledgePoints.stream()
                .filter(kp -> kp.getId().equals(knowledgePointId))
                .collect(Collectors.toList());
        }
        
        if (isCourseValid) {
            analysis.setCourseName(courseName);
        } else {
            analysis.setCourseName("课程不存在或无权访问");
        }
        
        // 获取目标学生列表（根据筛选条件）
        List<Long> targetStudentIds = getTargetStudentIds(courseId, classId, studentId, teacherId);
        
        // 如果没有学生，返回空数据
        if (targetStudentIds.isEmpty()) {
            analysis.setKnowledgePointDistribution(new ArrayList<>());
            analysis.setWeakTopics(new ArrayList<>());
            analysis.setAtRiskStudents(new ArrayList<>());
            return analysis;
        }
        
        // 获取知识点ID列表
        List<Long> knowledgePointIds = knowledgePoints.stream()
            .map(KnowledgePoint::getId)
            .collect(Collectors.toList());
        
        if (knowledgePointIds.isEmpty()) {
            analysis.setKnowledgePointDistribution(new ArrayList<>());
            analysis.setWeakTopics(new ArrayList<>());
            analysis.setAtRiskStudents(new ArrayList<>());
            return analysis;
        }
        
        // 获取学生的掌握情况数据
        List<Map<String, Object>> masteryData = knowledgePointMapper.getStudentMasteryByKnowledgePoints(knowledgePointIds, targetStudentIds);
        
        // 构建学生-知识点掌握率映射
        // Map<studentId, Map<knowledgePointId, masteryRate>>
        Map<Long, Map<Long, Double>> studentMasteryMap = new HashMap<>();
        for (Map<String, Object> item : masteryData) {
            Long sid = ((Number) item.get("student_id")).longValue();
            Long kpId = ((Number) item.get("knowledge_point_id")).longValue();
            Double rate = ((Number) item.get("mastery_rate")).doubleValue();
            studentMasteryMap.computeIfAbsent(sid, k -> new HashMap<>()).put(kpId, rate);
        }
        
        // 计算每个知识点的平均掌握率（只计算有记录的学生）
        Map<Long, Double> avgMasteryRateMap = new HashMap<>();
        Map<Long, Integer> assessedStudentCountMap = new HashMap<>();
        int totalStudentCount = targetStudentIds.size();
        
        for (Long kpId : knowledgePointIds) {
            double totalRate = 0.0;
            int assessedCount = 0;
            
            for (Long sid : targetStudentIds) {
                Map<Long, Double> studentKpMap = studentMasteryMap.get(sid);
                if (studentKpMap != null && studentKpMap.containsKey(kpId)) {
                    totalRate += studentKpMap.get(kpId);
                    assessedCount++;
                }
            }
            
            // 平均掌握率 = 有记录的学生的平均值（如果没有记录则为0%）
            double avgRate = assessedCount > 0 ? totalRate / assessedCount : 0.0;
            avgMasteryRateMap.put(kpId, avgRate);
            assessedStudentCountMap.put(kpId, assessedCount);
        }
        
        // 构建知识点分布数据
        List<KnowledgePointAnalysisDTO.KnowledgePointDistributionDTO> knowledgePointDistribution = new ArrayList<>();
        for (KnowledgePoint kp : knowledgePoints) {
            KnowledgePointAnalysisDTO.KnowledgePointDistributionDTO dto = new KnowledgePointAnalysisDTO.KnowledgePointDistributionDTO();
            dto.setKnowledgePointId(kp.getId());
            dto.setKnowledgePointName(kp.getPointName());
            dto.setMasteryRate(avgMasteryRateMap.getOrDefault(kp.getId(), 0.0));
            dto.setDifficulty(kp.getDifficulty());
            dto.setOrderIndex(kp.getOrderIndex());
            knowledgePointDistribution.add(dto);
        }
        analysis.setKnowledgePointDistribution(knowledgePointDistribution);
        
        // 构建薄弱知识点数据（平均掌握率<60%）
        List<KnowledgePointAnalysisDTO.WeakTopicDTO> weakTopics = new ArrayList<>();
        for (KnowledgePoint kp : knowledgePoints) {
            Double avgRate = avgMasteryRateMap.getOrDefault(kp.getId(), 0.0);
            if (avgRate < 60) {
                KnowledgePointAnalysisDTO.WeakTopicDTO dto = new KnowledgePointAnalysisDTO.WeakTopicDTO();
                dto.setKnowledgePointId(kp.getId());
                dto.setKnowledgePointName(kp.getPointName());
                dto.setAverageMastery(avgRate);
                dto.setStudentCount(assessedStudentCountMap.getOrDefault(kp.getId(), 0));
                dto.setDifficulty(kp.getDifficulty());
                weakTopics.add(dto);
            }
        }
        analysis.setWeakTopics(weakTopics);
        
        // 构建需要关注的学生数据
        // 待改进学生 = 有任一知识点掌握率<60% 或 有知识点未评估的学生
        List<KnowledgePointAnalysisDTO.AtRiskStudentDTO> atRiskStudents = new ArrayList<>();
        Set<Long> atRiskStudentIds = new HashSet<>();
        
        for (Long sid : targetStudentIds) {
            Map<Long, Double> studentKpMap = studentMasteryMap.getOrDefault(sid, new HashMap<>());
            boolean isAtRisk = false;
            List<KnowledgePointAnalysisDTO.WeakKnowledgePointDTO> weakKnowledgePoints = new ArrayList<>();
            
            for (KnowledgePoint kp : knowledgePoints) {
                Double rate = studentKpMap.get(kp.getId());
                if (rate == null) {
                    // 未评估，视为待改进
                    isAtRisk = true;
                    KnowledgePointAnalysisDTO.WeakKnowledgePointDTO weakKP = new KnowledgePointAnalysisDTO.WeakKnowledgePointDTO();
                    weakKP.setKnowledgePointId(kp.getId());
                    weakKP.setKnowledgePointName(kp.getPointName());
                    weakKP.setMasteryRate(0.0); // 未评估视为0%
                    weakKnowledgePoints.add(weakKP);
                } else if (rate < 60) {
                    // 掌握率低于60%
                    isAtRisk = true;
                    KnowledgePointAnalysisDTO.WeakKnowledgePointDTO weakKP = new KnowledgePointAnalysisDTO.WeakKnowledgePointDTO();
                    weakKP.setKnowledgePointId(kp.getId());
                    weakKP.setKnowledgePointName(kp.getPointName());
                    weakKP.setMasteryRate(rate);
                    weakKnowledgePoints.add(weakKP);
                }
            }
            
            if (isAtRisk) {
                atRiskStudentIds.add(sid);
            }
        }
        
        // 获取学生信息
        if (!atRiskStudentIds.isEmpty()) {
            List<Map<String, Object>> studentInfoList = knowledgePointMapper.getStudentInfoByIds(new ArrayList<>(atRiskStudentIds));
            Map<Long, String> studentNameMap = new HashMap<>();
            for (Map<String, Object> info : studentInfoList) {
                Long sid = ((Number) info.get("id")).longValue();
                String name = (String) info.get("name");
                studentNameMap.put(sid, name);
            }
            
            for (Long sid : atRiskStudentIds) {
                KnowledgePointAnalysisDTO.AtRiskStudentDTO dto = new KnowledgePointAnalysisDTO.AtRiskStudentDTO();
                dto.setStudentId(sid);
                dto.setStudentName(studentNameMap.getOrDefault(sid, "学生" + sid));
                
                // 获取该学生的薄弱知识点
                Map<Long, Double> studentKpMap = studentMasteryMap.getOrDefault(sid, new HashMap<>());
                List<KnowledgePointAnalysisDTO.WeakKnowledgePointDTO> weakKnowledgePoints = new ArrayList<>();
                
                for (KnowledgePoint kp : knowledgePoints) {
                    Double rate = studentKpMap.get(kp.getId());
                    if (rate == null || rate < 60) {
                        KnowledgePointAnalysisDTO.WeakKnowledgePointDTO weakKP = new KnowledgePointAnalysisDTO.WeakKnowledgePointDTO();
                        weakKP.setKnowledgePointId(kp.getId());
                        weakKP.setKnowledgePointName(kp.getPointName());
                        weakKP.setMasteryRate(rate != null ? rate : 0.0);
                        weakKnowledgePoints.add(weakKP);
                    }
                }
                
                dto.setWeakKnowledgePoints(weakKnowledgePoints);
                atRiskStudents.add(dto);
            }
        }
        
        analysis.setAtRiskStudents(atRiskStudents);
        
        return analysis;
    }
    
    /**
     * 根据筛选条件获取目标学生ID列表
     */
    private List<Long> getTargetStudentIds(Long courseId, Long classId, Long studentId, Long teacherId) {
        // 如果指定了单个学生
        if (studentId != null) {
            return Collections.singletonList(studentId);
        }
        
        // 如果指定了班级
        if (classId != null) {
            return knowledgePointMapper.getStudentIdsByClassId(classId);
        }
        
        // 如果指定了课程
        if (courseId != null) {
            return knowledgePointMapper.getStudentIdsByCourseId(courseId);
        }
        
        // 否则获取教师所有课程班级的学生
        return knowledgePointMapper.getStudentIdsByTeacherId(teacherId);
    }
}
