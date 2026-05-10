package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.dto.KnowledgePointAnalysisDTO;

public interface KnowledgePointAnalysisService {
    KnowledgePointAnalysisDTO getKnowledgePointAnalysis(Long courseId, Long classId, Long studentId, Long knowledgePointId, Long teacherId);
}