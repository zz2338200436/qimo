package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamDetailLookupToolTest {

    private final ExamFeignClient examClient = mock(ExamFeignClient.class);
    private final ExamDetailLookupTool tool = new ExamDetailLookupTool(examClient);

    @Test
    void intentIsExamDetailLookup() {
        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_EXAM_DETAIL);
    }

    @Test
    void getsTeacherExamDetailThroughExamService() {
        ExamDTO exam = new ExamDTO();
        exam.setId(9001L);
        exam.setTitle("Java期末考试");
        when(examClient.getTeacherExam(9001L, 7L)).thenReturn(exam);

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("examId", 9001L));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("exam")).isSameAs(exam);
        verify(examClient).getTeacherExam(9001L, 7L);
    }

    @Test
    void rejectsLookupWhenExamIdIsMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "查询考试详情需要 examId。");
        verify(examClient, never()).getTeacherExam(null, null);
    }
}
