package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherKnowledgePointEdgeClient;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgePointQueryToolTest {

    private final TeacherKnowledgePointEdgeClient knowledgePointClient = mock(TeacherKnowledgePointEdgeClient.class);
    private final KnowledgePointQueryTool tool = new KnowledgePointQueryTool(knowledgePointClient);

    @Test
    void forwardsCourseIdToCourseService() {
        List<Map<String, Object>> points = List.of(Map.of(
                "id", 501L,
                "pointName", "服务注册与发现",
                "courseId", 91005L,
                "difficulty", "中等"));
        when(knowledgePointClient.listKnowledgePoints("7", 91005L))
                .thenReturn(ResponseResult.success(points, "获取知识点列表成功", 200));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("courseId", "91005"));

        assertThat(result).containsEntry("status", "EXECUTED")
                .containsEntry("knowledgePoints", points)
                .containsEntry("message", "获取知识点列表成功");
        verify(knowledgePointClient).listKnowledgePoints("7", 91005L);
    }

    @Test
    void rejectsMissingCourseBeforeRemoteCall() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result).containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "缺少课程ID。");
        verifyNoInteractions(knowledgePointClient);
    }
}
