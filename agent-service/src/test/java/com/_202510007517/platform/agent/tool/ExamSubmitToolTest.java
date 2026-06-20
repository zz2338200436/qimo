package com._202510007517.platform.agent.tool;

import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
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

class ExamSubmitToolTest {

    private final ExamFeignClient examClient = mock(ExamFeignClient.class);
    private final ExamSubmitTool tool = new ExamSubmitTool(examClient);

    @Test
    void submitsExamOnlyWhenExamIsVisibleToStudent() {
        ExamDTO visibleExam = new ExamDTO();
        visibleExam.setId(100L);
        when(examClient.listByStudent(7L)).thenReturn(List.of(visibleExam));
        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setId(300L);
        when(examClient.submit(eq(100L), any())).thenReturn(submission);

        Map<String, Object> result = tool.execute(7L, "STUDENT", submitRequest(100L));

        assertThat(result).containsEntry("status", "EXECUTED");
        verify(examClient).submit(eq(100L), any());
    }

    @Test
    void rejectsExamSubmissionWhenExamIsNotVisibleToStudent() {
        ExamDTO visibleExam = new ExamDTO();
        visibleExam.setId(200L);
        when(examClient.listByStudent(7L)).thenReturn(List.of(visibleExam));

        Map<String, Object> result = tool.execute(7L, "STUDENT", submitRequest(100L));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "当前学生无权提交该考试。");
        verify(examClient, never()).submit(any(), any());
    }

    private static Map<String, Object> submitRequest(Long examId) {
        return Map.of(
                "examId", examId,
                "timeTaken", 45,
                "answers", Map.of("1", "A")
        );
    }
}
