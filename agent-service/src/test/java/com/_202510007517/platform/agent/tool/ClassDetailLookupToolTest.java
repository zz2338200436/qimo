package com._202510007517.platform.agent.tool;

import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassDetailLookupToolTest {

    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final ClassDetailLookupTool tool = new ClassDetailLookupTool(courseClient);

    @Test
    void getsClassDetailThroughCourseService() {
        Map<String, Object> classDetail = Map.of(
                "id", 3001L,
                "className", "软件工程 1 班",
                "studentCount", 42
        );
        when(courseClient.getClass(7L, 3001L)).thenReturn(classDetail);

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("classId", 3001L));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("class")).isSameAs(classDetail);
        verify(courseClient).getClass(7L, 3001L);
    }

    @Test
    void rejectsLookupWhenClassIdIsMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "查询班级详情需要 classId。");
        verify(courseClient, never()).getClass(null, null);
    }
}
