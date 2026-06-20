package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentSubmissionLookupToolTest {

    private final TeacherAssignmentEdgeClient assignmentClient = mock(TeacherAssignmentEdgeClient.class);
    private final AssignmentSubmissionLookupTool tool = new AssignmentSubmissionLookupTool(assignmentClient);

    @Test
    void listsAssignmentSubmissionsThroughAssignmentService() {
        AssignmentSubmissionDTO submission = new AssignmentSubmissionDTO();
        submission.setId(3001L);
        submission.setAssignmentId(2001L);
        submission.setStudentId(42L);
        when(assignmentClient.listAssignmentSubmissions("7", 2001L))
                .thenReturn(ResponseResult.success(List.of(submission)));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("assignmentId", 2001L));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("submissions")).isEqualTo(List.of(submission));
        verify(assignmentClient).listAssignmentSubmissions("7", 2001L);
    }

    @Test
    void rejectsLookupWhenAssignmentIdIsMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "查询作业提交记录需要 assignmentId。");
        verify(assignmentClient, never()).listAssignmentSubmissions(null, null);
    }
}
