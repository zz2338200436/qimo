package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAnalysisEdgeClient;
import com._202510007517.platform.agent.client.TeacherKnowledgeAnalysisEdgeClient;
import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeMasteryQueryToolTest {

    private final TeacherAnalysisEdgeClient analysisClient = mock(TeacherAnalysisEdgeClient.class);
    private final TeacherKnowledgeAnalysisEdgeClient knowledgeAnalysisClient = mock(TeacherKnowledgeAnalysisEdgeClient.class);
    private final KnowledgeMasteryQueryTool tool = new KnowledgeMasteryQueryTool(analysisClient, knowledgeAnalysisClient);

    @Test
    void forwardsStudentAndCourseToAnalysisService() {
        KnowledgeMasteryDTO mastery = new KnowledgeMasteryDTO(
                21L,
                3L,
                2L,
                5L,
                new BigDecimal("0.82"),
                4,
                "EXAM",
                1001L,
                "evt-1",
                Instant.parse("2026-06-13T08:00:00Z"));
        when(analysisClient.listStudentKnowledgeMastery("7", 21L, 3L))
                .thenReturn(ResponseResult.success(List.of(mastery)));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "studentId", "21",
                "courseId", 3));

        assertThat(result).containsEntry("status", "EXECUTED")
                .containsEntry("knowledgeMastery", List.of(mastery));
        verify(analysisClient).listStudentKnowledgeMastery("7", 21L, 3L);
    }

    @Test
    void rejectsMissingCourseWhenStudentDetailQueryLacksCourseId() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("studentId", "21"));

        assertThat(result).containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "缺少课程ID。");
        verifyNoInteractions(analysisClient, knowledgeAnalysisClient);
    }

    @Test
    void usesAggregateTeacherKnowledgeAnalysisWhenStudentIdIsMissing() {
        when(knowledgeAnalysisClient.getKnowledgePointAnalysis("7", 3L, null, null, null))
                .thenReturn(ResponseResult.success(Map.of("courseName", "课程 3")));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("courseId", 3));

        assertThat(result).containsEntry("status", "EXECUTED")
                .containsEntry("knowledgeAnalysis", Map.of("courseName", "课程 3"));
        verify(knowledgeAnalysisClient).getKnowledgePointAnalysis("7", 3L, null, null, null);
        verifyNoInteractions(analysisClient);
    }
}
