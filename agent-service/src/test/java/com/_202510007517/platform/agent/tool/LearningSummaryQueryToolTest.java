package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAnalysisEdgeClient;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningSummaryQueryToolTest {

    private final TeacherAnalysisEdgeClient analysisClient = mock(TeacherAnalysisEdgeClient.class);
    private final LearningSummaryQueryTool tool = new LearningSummaryQueryTool(analysisClient);

    @Test
    void forwardsLearningSummaryFiltersToAnalysisService() {
        when(analysisClient.getLearningSummary("7", 101L, 202L, "month"))
                .thenReturn(ResponseResult.success(Map.of("totalStudents", 36)));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "classId", "101",
                "courseId", 202L,
                "timeRange", "month"
        ));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsKey("learningSummary");
        verify(analysisClient).getLearningSummary("7", 101L, 202L, "month");
    }
}
