package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherSubmissionEdgeClient;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentGradeRequestDTO;
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

class AssignmentGradeToolTest {

    private final TeacherSubmissionEdgeClient submissionClient = mock(TeacherSubmissionEdgeClient.class);
    private final AssignmentGradeTool tool = new AssignmentGradeTool(submissionClient);

    @Test
    void gradesSubmissionThroughAssignmentService() {
        AssignmentSubmissionDTO submission = new AssignmentSubmissionDTO();
        submission.setId(3001L);
        submission.setScore(95);
        submission.setGraded(true);
        when(submissionClient.gradeSubmission(eq("7"), eq(3001L), any(TeacherAssignmentGradeRequestDTO.class)))
                .thenReturn(ResponseResult.success(submission));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "submissionId", 3001L,
                "score", 95,
                "teacherComment", "完成度高"
        ));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("submission")).isSameAs(submission);
        verify(submissionClient).gradeSubmission(eq("7"), eq(3001L), any(TeacherAssignmentGradeRequestDTO.class));
    }

    @Test
    void rejectsGradeWhenRequiredSlotsAreMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("score", 95));

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "批改作业需要 submissionId、score。");
        verify(submissionClient, never()).gradeSubmission(any(), any(), any());
    }
}
