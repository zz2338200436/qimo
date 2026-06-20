package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAnalysisEdgeClient;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherDashboardQueryToolTest {

    private final TeacherAnalysisEdgeClient analysisClient = mock(TeacherAnalysisEdgeClient.class);
    private final TeacherDashboardQueryTool tool = new TeacherDashboardQueryTool(analysisClient);

    @Test
    void forwardsTeacherDashboardFiltersToAnalysisService() {
        when(analysisClient.getTeacherDashboard("7", 101L, 202L, "30d"))
                .thenReturn(ResponseResult.success(Map.of("totalStudents", 36)));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "classId", "101",
                "courseId", 202L,
                "timeRange", "30d"
        ));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsKey("teacherDashboard");
        verify(analysisClient).getTeacherDashboard("7", 101L, 202L, "30d");
    }
}
