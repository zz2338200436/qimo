package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentDetailLookupToolTest {

    private final TeacherAssignmentEdgeClient assignmentClient = mock(TeacherAssignmentEdgeClient.class);
    private final AssignmentDetailLookupTool tool = new AssignmentDetailLookupTool(assignmentClient);

    @Test
    void getsAssignmentDetailThroughAssignmentService() {
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(2001L);
        assignment.setTitle("Spring Cloud实验");
        when(assignmentClient.getAssignment("7", 2001L))
                .thenReturn(ResponseResult.success(assignment));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("assignmentId", 2001L));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("assignment")).isSameAs(assignment);
        verify(assignmentClient).getAssignment("7", 2001L);
    }

    @Test
    void rejectsLookupWhenAssignmentIdIsMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "查询作业详情需要 assignmentId。");
        verify(assignmentClient, never()).getAssignment(null, null);
    }
}
