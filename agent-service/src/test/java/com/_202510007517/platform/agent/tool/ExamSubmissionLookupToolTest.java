package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherExamEdgeClient;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamSubmissionLookupToolTest {

    private final TeacherExamEdgeClient examClient = mock(TeacherExamEdgeClient.class);
    private final ExamSubmissionLookupTool tool = new ExamSubmissionLookupTool(examClient);

    @Test
    void listsExamSubmissionsThroughExamService() {
        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        when(examClient.listExamSubmissions("7", 9001L))
                .thenReturn(ResponseResult.success(List.of(submission)));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("examId", 9001L));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("submissions")).isEqualTo(List.of(submission));
        verify(examClient).listExamSubmissions("7", 9001L);
    }

    @Test
    void rejectsLookupWhenExamIdIsMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "查询考试提交记录需要 examId。");
        verify(examClient, never()).listExamSubmissions(null, null);
    }
}
