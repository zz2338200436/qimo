package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherExamEdgeClient;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamGradeRequestDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamGradeToolTest {

    private final TeacherExamEdgeClient examClient = mock(TeacherExamEdgeClient.class);
    private final ExamGradeTool tool = new ExamGradeTool(examClient);

    @Test
    void gradesSubmissionThroughExamService() {
        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setId(9101L);
        submission.setScore(92);
        submission.setGraded(true);
        when(examClient.gradeSubmission(eq("7"), eq(9101L), any(TeacherExamGradeRequestDTO.class)))
                .thenReturn(ResponseResult.success(submission));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "submissionId", 9101L,
                "score", 92,
                "teacherComment", "答题完整"
        ));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("submission")).isSameAs(submission);

        ArgumentCaptor<TeacherExamGradeRequestDTO> captor = ArgumentCaptor.forClass(TeacherExamGradeRequestDTO.class);
        verify(examClient).gradeSubmission(eq("7"), eq(9101L), captor.capture());
        assertThat(captor.getValue().getScore()).isEqualTo(92);
        assertThat(captor.getValue().getTeacherComment()).isEqualTo("答题完整");
        assertThat(captor.getValue().getGraded()).isTrue();
    }

    @Test
    void rejectsGradeWhenRequiredSlotsAreMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("score", 92));

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "批改考试需要 submissionId、score。");
        verify(examClient, never()).gradeSubmission(any(), any(), any());
    }
}
