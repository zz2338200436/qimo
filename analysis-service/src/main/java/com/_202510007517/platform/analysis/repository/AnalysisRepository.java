package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AnalysisRepository {

    void upsertScoreTrend(ScoreTrendRecord record);

    void upsertKnowledgeMastery(KnowledgeMasteryRecord record);

    List<ScoreTrendDTO> listScoreTrends(Long classId, Long courseId, Instant since);

    List<KnowledgeMasteryDTO> listKnowledgeMastery(Long studentId, Long courseId);

    List<KnowledgeMasteryDTO> listKnowledgeMasteryByScope(Long classId, Long courseId);

    Map<String, Object> getTeacherDashboard(Long teacherId, Long classId, Long courseId, String timeRange);

    Map<String, Object> getTeacherLearningSummary(Long teacherId, Long classId, Long courseId, String timeRange);

    List<Map<String, Object>> listStudentStudyTimeDistribution(
            Long studentId,
            String type,
            String semester,
            Long courseId,
            String timeRange);

    Map<String, Object> getStudentLearningStats(
            Long studentId,
            String semester,
            Long courseId,
            String timeRange);

    List<Map<String, Object>> listStudentKnowledgePoints(
            Long studentId,
            String semester,
            Long courseId,
            String timeRange);

    Map<String, Object> getStudentKnowledgePointDetail(Long studentId, Long knowledgePointId);
}
