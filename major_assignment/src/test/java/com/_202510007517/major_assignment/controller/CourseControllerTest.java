package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.config.MultiRoleSessionFilter;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseControllerTest {

    @Test
    void getCourses_appliesServerSidePaginationAndBoundsOutOfRangePage() {
        CourseController controller = new CourseController();
        CourseService courseService = mock(CourseService.class);
        ReflectionTestUtils.setField(controller, "courseService", courseService);

        List<Course> courses = List.of(
                buildCourse(1L, "课程一", "C-001"),
                buildCourse(2L, "课程二", "C-002"),
                buildCourse(3L, "课程三", "C-003")
        );
        when(courseService.findByTeacherIdWithSearch(7L, null, null, null, null)).thenReturn(courses);

        ResponseResult<PageResult<Course>> response = controller.getCourses(
                loggedInRequest(7L), 9, 2, "id", "DESC", null, null, null, null);

        assertThat(response.isSuccess()).isTrue();
        PageResult<Course> page = response.getData();
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getPageSize()).isEqualTo(2);
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isTrue();
        assertThat(page.getOffset()).isEqualTo(2);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.isEmpty()).isFalse();
        assertThat(page.getContent()).extracting(Course::getId).containsExactly(3L);
    }

    @Test
    void controllerUsesClassLevelTeacherGuard() {
        RequireLogin requireLogin = CourseController.class.getAnnotation(RequireLogin.class);

        assertThat(requireLogin).isNotNull();
        assertThat(requireLogin.roles()).containsExactly(RoleConstants.TEACHER);
    }

    @Test
    void controllerMethodsShouldNotDuplicateTeacherGuardAtMethodLevel() {
        for (Method method : CourseController.class.getDeclaredMethods()) {
            if (method.getName().startsWith("lambda$")) {
                continue;
            }
            assertThat(method.getAnnotation(RequireLogin.class))
                    .as("course controller method %s should rely on class-level guard", method.getName())
                    .isNull();
        }
    }

    private static Course buildCourse(Long id, String name, String code) {
        Course course = new Course();
        course.setId(id);
        course.setCourseName(name);
        course.setCourseCode(code);
        course.setCredit(3);
        course.setTotalHours(32);
        course.setCourseCategory("核心课");
        course.setCourseStatus("进行中");
        course.setStudentCount(10);
        return course;
    }

    private static MockHttpServletRequest loggedInRequest(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                MultiRoleSessionFilter.CURRENT_USER_ATTR,
                MultiRoleSessionFilter.AuthUser.of(userId, List.of("TEACHER")));
        return request;
    }
}
