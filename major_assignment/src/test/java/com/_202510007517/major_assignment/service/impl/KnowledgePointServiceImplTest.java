package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.KnowledgePoint;
import com._202510007517.major_assignment.mapper.KnowledgePointMapper;
import com._202510007517.major_assignment.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgePointServiceImplTest {

    @Test
    void getKnowledgePointsByTeacherIdAggregatesTeacherCourseKnowledgePoints() {
        KnowledgePointMapper knowledgePointMapper = mock(KnowledgePointMapper.class);
        CourseService courseService = mock(CourseService.class);

        KnowledgePointServiceImpl service = new KnowledgePointServiceImpl();
        ReflectionTestUtils.setField(service, "knowledgePointMapper", knowledgePointMapper);
        ReflectionTestUtils.setField(service, "courseService", courseService);

        KnowledgePoint pointA = new KnowledgePoint();
        pointA.setId(8001L);
        pointA.setPointName("函数极限");
        pointA.setCourseId(2L);

        KnowledgePoint pointB = new KnowledgePoint();
        pointB.setId(8002L);
        pointB.setPointName("导数应用");
        pointB.setCourseId(2L);

        when(courseService.findCourseIdsByTeacherId(7L)).thenReturn(List.of(2L, 3L));
        when(knowledgePointMapper.findByCourseIds(List.of(2L, 3L))).thenReturn(List.of(pointA, pointB));

        List<Map<String, Object>> response = service.getKnowledgePointsByTeacherId(7L);

        assertThat(response)
                .extracting(item -> item.get("pointName"))
                .containsExactly("函数极限", "导数应用");
        assertThat(response)
                .extracting(item -> item.get("id"))
                .containsExactly(8001L, 8002L);
    }

    @Test
    void getKnowledgePointsByTeacherIdReturnsEmptyWhenTeacherHasNoCourses() {
        KnowledgePointMapper knowledgePointMapper = mock(KnowledgePointMapper.class);
        CourseService courseService = mock(CourseService.class);

        KnowledgePointServiceImpl service = new KnowledgePointServiceImpl();
        ReflectionTestUtils.setField(service, "knowledgePointMapper", knowledgePointMapper);
        ReflectionTestUtils.setField(service, "courseService", courseService);

        when(courseService.findCourseIdsByTeacherId(7L)).thenReturn(List.of());

        List<Map<String, Object>> response = service.getKnowledgePointsByTeacherId(7L);

        assertThat(response).isEmpty();
        verifyNoInteractions(knowledgePointMapper);
    }

    @Test
    void getKnowledgePointsByTeacherIdAndCourseIdReturnsEmptyForOtherTeachersCourse() throws Exception {
        KnowledgePointMapper knowledgePointMapper = mock(KnowledgePointMapper.class);
        CourseService courseService = mock(CourseService.class);

        KnowledgePointServiceImpl service = new KnowledgePointServiceImpl();
        ReflectionTestUtils.setField(service, "knowledgePointMapper", knowledgePointMapper);
        ReflectionTestUtils.setField(service, "courseService", courseService);

        when(courseService.findCourseIdsByTeacherId(7L)).thenReturn(List.of(2L));

        Method method = ReflectionUtils.findMethod(
                KnowledgePointServiceImpl.class,
                "getKnowledgePointsByTeacherId",
                Long.class,
                Long.class
        );

        assertThat(method).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> response = (List<Map<String, Object>>) method.invoke(service, 7L, 99L);

        assertThat(response).isEmpty();
        verifyNoInteractions(knowledgePointMapper);
    }
}
