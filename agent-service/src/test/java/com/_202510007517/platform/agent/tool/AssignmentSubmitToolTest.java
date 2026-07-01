package com._202510007517.platform.agent.tool;

import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentSubmitToolTest {

    private final AssignmentFeignClient assignmentClient = mock(AssignmentFeignClient.class);
    private final AssignmentSubmitTool tool = new AssignmentSubmitTool(assignmentClient);

    @Test
    void submitsAssignmentOnlyWhenAssignmentIsVisibleToStudent() {
        AssignmentStudentScoreDTO visibleAssignment = new AssignmentStudentScoreDTO();
        visibleAssignment.setRelatedId(100L);
        when(assignmentClient.listStudentScores(7L)).thenReturn(List.of(visibleAssignment));
        AssignmentSubmissionDTO submission = new AssignmentSubmissionDTO();
        submission.setId(300L);
        when(assignmentClient.submit(eq(100L), any())).thenReturn(submission);

        Map<String, Object> result = tool.execute(7L, "STUDENT", submitRequest(100L));

        assertThat(result).containsEntry("status", "EXECUTED");
        verify(assignmentClient).submit(eq(100L), any());
    }

    @Test
    void submitsAssignmentWhenAssignmentIsVisibleInPendingAssignments() {
        when(assignmentClient.listStudentScores(7L)).thenReturn(List.of());
        when(assignmentClient.listStudentAssignments(7L, 1, 100, "dueDate", "DESC", null, false, true))
                .thenReturn(Map.of("content", List.of(Map.of("id", 100L, "title", "实验报告"))));
        AssignmentSubmissionDTO submission = new AssignmentSubmissionDTO();
        submission.setId(300L);
        when(assignmentClient.submit(eq(100L), any())).thenReturn(submission);

        Map<String, Object> result = tool.execute(7L, "STUDENT", submitRequest(100L));

        assertThat(result).containsEntry("status", "EXECUTED");
        verify(assignmentClient).submit(eq(100L), any());
    }

    @Test
    void rejectsAssignmentSubmissionWhenAssignmentIsNotVisibleToStudent() {
        AssignmentStudentScoreDTO visibleAssignment = new AssignmentStudentScoreDTO();
        visibleAssignment.setRelatedId(200L);
        when(assignmentClient.listStudentScores(7L)).thenReturn(List.of(visibleAssignment));

        Map<String, Object> result = tool.execute(7L, "STUDENT", submitRequest(100L));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "当前学生无权提交该作业。");
        verify(assignmentClient, never()).submit(any(), any());
    }

    private static Map<String, Object> submitRequest(Long assignmentId) {
        return Map.of(
                "assignmentId", assignmentId,
                "content", "实验报告已完成"
        );
    }
}
