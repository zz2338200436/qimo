package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
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

class AssignmentUpdateToolTest {

    private final TeacherAssignmentEdgeClient assignmentClient = mock(TeacherAssignmentEdgeClient.class);
    private final AssignmentUpdateTool tool = new AssignmentUpdateTool(assignmentClient);

    @Test
    void updatesAssignmentThroughTeacherAssignmentService() {
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(3001L);
        assignment.setTitle("Spring Cloud进阶实验");
        when(assignmentClient.updateAssignment(eq("7"), eq(3001L), any(TeacherAssignmentUpsertRequestDTO.class)))
                .thenReturn(ResponseResult.success(assignment));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "assignmentId", 3001L,
                "title", "Spring Cloud进阶实验",
                "courseId", 100L,
                "dueDate", "2026-06-20 22:00",
                "maxScore", 100
        ));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("assignment")).isSameAs(assignment);
        verify(assignmentClient).updateAssignment(eq("7"), eq(3001L), any(TeacherAssignmentUpsertRequestDTO.class));
    }

    @Test
    void rejectsAssignmentUpdateWhenRequiredSlotsAreMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "title", "Spring Cloud进阶实验"
        ));

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "更新作业需要 assignmentId、title、courseId、dueDate、maxScore。");
        verify(assignmentClient, never()).updateAssignment(any(), any(), any());
    }
}
