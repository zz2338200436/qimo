package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentDeleteToolTest {

    private final TeacherAssignmentEdgeClient assignmentClient = mock(TeacherAssignmentEdgeClient.class);
    private final AssignmentDeleteTool tool = new AssignmentDeleteTool(assignmentClient);

    @Test
    void deletesAssignmentThroughTeacherAssignmentService() {
        when(assignmentClient.deleteAssignment(eq("7"), eq(3001L))).thenReturn(ResponseResult.noContent());

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("assignmentId", 3001L));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("assignmentId", 3001L);
        verify(assignmentClient).deleteAssignment(eq("7"), eq(3001L));
    }

    @Test
    void rejectsAssignmentDeleteWhenAssignmentIdIsMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "删除作业需要 assignmentId。");
        verify(assignmentClient, never()).deleteAssignment(any(), any());
    }
}
