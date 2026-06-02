package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import com._202510007517.major_assignment.service.ExamService;
import com._202510007517.major_assignment.service.ExamSubmissionService;
import com._202510007517.major_assignment.service.KnowledgePointService;
import com._202510007517.major_assignment.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamControllerTest {

    @Test
    void controllerRequiresTeacherLoginAtClassLevel() {
        RequireLogin requireLogin = ExamController.class.getAnnotation(RequireLogin.class);

        assertThat(requireLogin).isNotNull();
        assertThat(requireLogin.roles()).containsExactly(RoleConstants.TEACHER);
    }

    @Test
    void noMethodShouldRepeatRequireLoginAnnotationOnceClassLevelGuardExists() {
        for (Method method : ExamController.class.getDeclaredMethods()) {
            if (!method.getName().startsWith("lambda$")) {
                assertThat(method.getAnnotation(RequireLogin.class))
                        .as("method %s should rely on class-level teacher guard", method.getName())
                        .isNull();
            }
        }
    }

    @Test
    void getAllSubmissions_usesBoundedPaginationAndSpringPageResponseShape() {
        ExamController controller = new ExamController();
        ExamSubmissionService examSubmissionService = mock(ExamSubmissionService.class);

        ReflectionTestUtils.setField(controller, "examService", mock(ExamService.class));
        ReflectionTestUtils.setField(controller, "examSubmissionService", examSubmissionService);
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "notificationService", mock(NotificationService.class));
        ReflectionTestUtils.setField(controller, "earlyWarningAnalysisService", mock(EarlyWarningAnalysisService.class));
        ReflectionTestUtils.setField(controller, "knowledgePointService", mock(KnowledgePointService.class));
        ReflectionTestUtils.setField(controller, "examMapper", mock(com._202510007517.major_assignment.mapper.ExamMapper.class));

        ExamSubmission submission = new ExamSubmission();
        submission.setId(88L);
        submission.setExamId(9L);
        submission.setStudentId(6L);
        submission.setSubmissionDate(new Date());

        when(examSubmissionService.countSubmissions(null, null, null)).thenReturn(3);
        when(examSubmissionService.getSubmissionsWithPagination(2, 2, 3, "id", "DESC", null, null, null))
                .thenReturn(List.of(submission));

        ResponseResult<Map<String, Object>> response = controller.getAllSubmissions(
                9, 2, "id", "DESC", null, null, null, loggedInRequest(7L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsEntry("totalElements", 3L);
        assertThat(response.getData()).containsEntry("totalPages", 2);
        assertThat(response.getData()).containsEntry("number", 1);
        assertThat(response.getData()).containsEntry("size", 2);
        assertThat(response.getData()).containsEntry("last", true);
        assertThat(response.getData()).containsEntry("first", false);

        @SuppressWarnings("unchecked")
        List<ExamSubmission> content = (List<ExamSubmission>) response.getData().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).getId()).isEqualTo(88L);

        @SuppressWarnings("unchecked")
        Map<String, Object> pageable = (Map<String, Object>) response.getData().get("pageable");
        assertThat(pageable)
                .containsEntry("pageNumber", 1)
                .containsEntry("pageSize", 2)
                .containsEntry("offset", 2);

        verify(examSubmissionService)
                .getSubmissionsWithPagination(2, 2, 3, "id", "DESC", null, null, null);
    }

    private static MockHttpServletRequest loggedInRequest(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                com._202510007517.major_assignment.config.MultiRoleSessionFilter.CURRENT_USER_ATTR,
                com._202510007517.major_assignment.config.MultiRoleSessionFilter.AuthUser.of(userId, List.of("TEACHER")));
        return request;
    }
}
