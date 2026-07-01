package com._202510007517.platform.agent.tool;

import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExamUpdateToolTest {

    private final ExamFeignClient examClient = mock(ExamFeignClient.class);
    private final ExamUpdateTool tool = new ExamUpdateTool(examClient);

    @Test
    void updatesTeacherOwnedExamThroughExamService() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", updateRequest(9002L));

        assertThat(result).containsEntry("status", "EXECUTED");
        verify(examClient).updateTeacherExam(eq(9002L), eq(7L), any(TeacherExamUpsertRequestDTO.class));
    }

    @Test
    void rejectsUpdateWithoutExamId() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "title", "Java期末考试",
                "courseId", 100L,
                "startTime", "2026-06-20T09:00",
                "endTime", "2026-06-20T10:30",
                "duration", 90
        ));

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "更新考试需要 examId、title、courseId、startTime、endTime、duration。");
    }

    private static Map<String, Object> updateRequest(Long examId) {
        return Map.of(
                "examId", examId,
                "title", "Java期末考试",
                "description", "闭卷考试",
                "courseId", 100L,
                "startTime", "2026-06-20T09:00",
                "endTime", "2026-06-20T10:30",
                "duration", 90,
                "isOnline", true
        );
    }
}
