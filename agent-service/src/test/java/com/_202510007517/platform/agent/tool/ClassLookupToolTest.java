package com._202510007517.platform.agent.tool;

import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassLookupToolTest {

    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final ClassLookupTool tool = new ClassLookupTool(courseClient);

    @Test
    void listsTeacherClassesThroughCourseService() {
        TeacherClassDTO classDTO = new TeacherClassDTO();
        classDTO.setId(3001L);
        classDTO.setClassName("软件工程 1 班");
        when(courseClient.listTeacherClasses(7L, null, null, null, null, null)).thenReturn(List.of(classDTO));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("classes")).isEqualTo(List.of(classDTO));
        verify(courseClient).listTeacherClasses(7L, null, null, null, null, null);
    }

    @Test
    void forwardsOptionalClassFiltersToCourseService() {
        TeacherClassDTO classDTO = new TeacherClassDTO();
        classDTO.setId(3002L);
        classDTO.setClassName("Java 2 班");
        when(courseClient.listTeacherClasses(7L, "Java", "2026", "软件工程", 12L, 1001L))
                .thenReturn(List.of(classDTO));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "className", "Java",
                "grade", "2026",
                "majorName", "软件工程",
                "majorId", 12L,
                "courseId", 1001L
        ));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("classes")).isEqualTo(List.of(classDTO));
        verify(courseClient).listTeacherClasses(7L, "Java", "2026", "软件工程", 12L, 1001L);
    }
}
