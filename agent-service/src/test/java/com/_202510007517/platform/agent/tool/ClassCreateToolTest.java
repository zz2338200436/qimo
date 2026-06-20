package com._202510007517.platform.agent.tool;

import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
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

class ClassCreateToolTest {

    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final ClassCreateTool tool = new ClassCreateTool(courseClient);

    @Test
    void createsClassThroughCourseServiceForTeacher() {
        when(courseClient.createClass(eq(7L), any(ClassUpsertRequestDTO.class))).thenReturn(501L);

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "className", "软件2301",
                "year", "2023",
                "capacity", 40,
                "courseId", 101L,
                "majorId", 2L,
                "classTime", "周一 1-2节",
                "classLocation", "教学楼A101"
        ));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("classId", 501L);
        ArgumentCaptor<ClassUpsertRequestDTO> captor = ArgumentCaptor.forClass(ClassUpsertRequestDTO.class);
        verify(courseClient).createClass(eq(7L), captor.capture());
        ClassUpsertRequestDTO request = captor.getValue();
        assertThat(request.getClassName()).isEqualTo("软件2301");
        assertThat(request.getYear()).isEqualTo("2023");
        assertThat(request.getCapacity()).isEqualTo(40);
        assertThat(request.getCourseId()).isEqualTo(101L);
        assertThat(request.getMajorId()).isEqualTo(2L);
        assertThat(request.getClassTime()).isEqualTo("周一 1-2节");
        assertThat(request.getClassLocation()).isEqualTo("教学楼A101");
    }

    @Test
    void rejectsClassCreateForNonTeacher() {
        Map<String, Object> result = tool.execute(42L, "STUDENT", Map.of(
                "className", "软件2301",
                "year", "2023",
                "capacity", 40
        ));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "只有教师可以创建班级。");
        verify(courseClient, never()).createClass(any(), any());
    }

    @Test
    void reportsMissingRequiredFieldsBeforeRemoteCall() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("className", "软件2301"));

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "创建班级需要 className、year、capacity。");
        verify(courseClient, never()).createClass(any(), any());
    }
}
