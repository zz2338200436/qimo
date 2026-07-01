package com._202510007517.platform.agent.tool;

import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ExamDeleteToolTest {

    private final ExamFeignClient examClient = mock(ExamFeignClient.class);
    private final ExamDeleteTool tool = new ExamDeleteTool(examClient);

    @Test
    void deletesTeacherOwnedExamThroughExamService() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("examId", 9002L));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("examId", 9002L);
        verify(examClient).deleteTeacherExam(eq(9002L), eq(7L));
    }

    @Test
    void rejectsDeleteWithoutExamId() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "删除考试需要 examId。");
        verify(examClient, never()).deleteTeacherExam(any(), any());
    }
}
