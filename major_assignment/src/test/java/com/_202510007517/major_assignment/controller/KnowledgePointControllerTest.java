package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.KnowledgePoint;
import com._202510007517.major_assignment.service.KnowledgePointService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgePointControllerTest {

    @Test
    void getKnowledgePointsAtRootReturnsTeacherVisibleKnowledgePoints() throws Exception {
        KnowledgePointController controller = new KnowledgePointController();
        ReflectionTestUtils.setField(controller, "knowledgePointService", new StubKnowledgePointService());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/teacher/knowledge-points").session(loggedInSession(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(9201))
                .andExpect(jsonPath("$.data[0].pointName").value("函数极限"))
                .andExpect(jsonPath("$.data[1].id").value(9202))
                .andExpect(jsonPath("$.data[1].pointName").value("导数应用"));
    }

    @Test
    void getKnowledgePointsAtRootWithCourseIdUsesTeacherVisibleScope() throws Exception {
        KnowledgePointController controller = new KnowledgePointController();
        StubKnowledgePointService service = new StubKnowledgePointService();
        ReflectionTestUtils.setField(controller, "knowledgePointService", service);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/teacher/knowledge-points")
                        .param("courseId", "99")
                        .session(loggedInSession(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));

        assertThat(service.teacherCourseScopeRequest).isEqualTo("7-99");
    }

    private static MockHttpSession loggedInSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }

    private static final class StubKnowledgePointService implements KnowledgePointService {

        private String teacherCourseScopeRequest;

        @Override
        public KnowledgePoint createKnowledgePoint(KnowledgePoint knowledgePoint) {
            return knowledgePoint;
        }

        @Override
        public KnowledgePoint updateKnowledgePoint(Long id, KnowledgePoint knowledgePoint) {
            return knowledgePoint;
        }

        @Override
        public void deleteKnowledgePoint(Long id) {
        }

        @Override
        public KnowledgePoint getKnowledgePointById(Long id) {
            return null;
        }

        @Override
        public List<Map<String, Object>> getKnowledgePointsByCourseId(Long courseId) {
            if (Long.valueOf(99L).equals(courseId)) {
                return List.of(Map.of(
                        "id", 9901L,
                        "pointName", "其他教师课程知识点",
                        "courseId", 99L
                ));
            }
            return List.of(Map.of(
                    "id", 9201L,
                    "pointName", "函数极限",
                    "courseId", 2L
            ));
        }

        @Override
        public List<Map<String, Object>> getKnowledgePointsByTeacherId(Long teacherId) {
            return List.of(
                    Map.of("id", 9201L, "pointName", "函数极限", "courseId", 2L),
                    Map.of("id", 9202L, "pointName", "导数应用", "courseId", 2L)
            );
        }

        public List<Map<String, Object>> getKnowledgePointsByTeacherId(Long teacherId, Long courseId) {
            teacherCourseScopeRequest = teacherId + "-" + courseId;
            if (Long.valueOf(2L).equals(courseId)) {
                return List.of(Map.of(
                        "id", 9201L,
                        "pointName", "函数极限",
                        "courseId", 2L
                ));
            }
            return List.of();
        }

        @Override
        public void setAssignmentKnowledgePoints(Long assignmentId, List<Long> knowledgePointIds) {
        }

        @Override
        public void setExamKnowledgePoints(Long examId, List<Long> knowledgePointIds) {
        }

        @Override
        public List<Map<String, Object>> getKnowledgePointsByAssignmentId(Long assignmentId) {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> getKnowledgePointsByExamId(Long examId) {
            return List.of();
        }

        @Override
        public void analyzeStudentKnowledgePointMastery(Long studentId, Long courseId) {
        }

        @Override
        public List<Map<String, Object>> getStudentKnowledgeMastery(Long studentId, Long courseId) {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> getKnowledgePointMasteryStats(Long courseId) {
            return List.of();
        }

        @Override
        public Map<String, Object> analyzeKnowledgePointMasteryForStudent(Long studentId, Long knowledgePointId) {
            return Map.of();
        }
    }
}
