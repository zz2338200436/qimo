package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAnalysisEdgeClient;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreTrendQueryToolTest {

    private final TeacherAnalysisEdgeClient analysisClient = mock(TeacherAnalysisEdgeClient.class);
    private final ScoreTrendQueryTool tool = new ScoreTrendQueryTool(analysisClient);

    @Test
    void forwardsTeacherFiltersToAnalysisService() {
        ScoreTrendDTO trend = new ScoreTrendDTO(
                21L,
                3L,
                2L,
                "EXAM",
                1001L,
                2001L,
                92,
                100,
                new BigDecimal("0.92"),
                Instant.parse("2026-06-13T08:00:00Z"));
        when(analysisClient.listScoreTrend("7", 2L, 3L, "semester"))
                .thenReturn(ResponseResult.success(List.of(trend)));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "classId", "2",
                "courseId", 3,
                "timeRange", "semester"));

        assertThat(result).containsEntry("status", "EXECUTED")
                .containsEntry("scoreTrend", List.of(trend));
        verify(analysisClient).listScoreTrend("7", 2L, 3L, "semester");
    }
}
