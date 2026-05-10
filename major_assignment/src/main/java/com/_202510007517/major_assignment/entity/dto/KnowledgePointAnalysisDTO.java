package com._202510007517.major_assignment.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class KnowledgePointAnalysisDTO {
    private String courseName;
    private List<KnowledgePointDistributionDTO> knowledgePointDistribution;
    private List<AtRiskStudentDTO> atRiskStudents;
    private List<WeakTopicDTO> weakTopics;
    
    @Data
    public static class KnowledgePointDistributionDTO {
        private Long knowledgePointId;
        private String knowledgePointName;
        private Double masteryRate;
        private String difficulty;
        private Integer orderIndex;
    }
    
    @Data
    public static class WeakTopicDTO {
        private Long knowledgePointId;
        private String knowledgePointName;
        private Double averageMastery;
        private Integer studentCount;
        private String difficulty;
    }
    
    @Data
    public static class AtRiskStudentDTO {
        private Long studentId;
        private String studentName;
        private List<WeakKnowledgePointDTO> weakKnowledgePoints;
    }
    
    @Data
    public static class WeakKnowledgePointDTO {
        private Long knowledgePointId;
        private String knowledgePointName;
        private Double masteryRate;
    }
}