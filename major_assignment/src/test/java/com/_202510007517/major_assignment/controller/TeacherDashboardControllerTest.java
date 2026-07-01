package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.config.MultiRoleSessionFilter;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.AssignmentSubmission;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.TeacherDashboardService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class TeacherDashboardControllerTest {

    @Test
    void controllerRequiresTeacherLoginAtClassLevel() {
        RequireLogin requireLogin = TeacherDashboardController.class.getAnnotation(RequireLogin.class);

        assertThat(requireLogin).isNotNull();
        assertThat(requireLogin.roles()).containsExactly(RoleConstants.TEACHER);
    }

    @Test
    void noMethodShouldRepeatRequireLoginAnnotationOnceClassLevelGuardExists() {
        for (Method method : TeacherDashboardController.class.getDeclaredMethods()) {
            if (!method.getName().startsWith("lambda$")) {
                assertThat(method.getAnnotation(RequireLogin.class))
                        .as("method %s should rely on class-level teacher guard", method.getName())
                        .isNull();
            }
        }
    }

    @Test
    void getStudentByIdUsesUserServiceProfileAndKeepsLocalPerformanceData() {
        TeacherDashboardController controller = new TeacherDashboardController();
        CourseMapper courseMapper = mock(CourseMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserService userService = mock(UserService.class);
        UserServiceProfileClient userServiceProfileClient = mock(UserServiceProfileClient.class);

        ReflectionTestUtils.setField(controller, "teacherDashboardService", mock(TeacherDashboardService.class));
        ReflectionTestUtils.setField(controller, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", userServiceProfileClient);

        StudentProfileDTO profile = new StudentProfileDTO();
        profile.setStudentId(42L);
        profile.setRealName("学生四二");
        profile.setClassName("未知班级");
        profile.setRoles(List.of("STUDENT"));
        when(courseMapper.getStudentIdsByClassTeacherId(9L)).thenReturn(List.of(42L));
        when(userServiceProfileClient.getStudentProfile(42L)).thenReturn(Optional.of(profile));
        when(studentMapper.getStudentPerformance(42L)).thenReturn(Map.of("averageScore", 91));

        ResponseResult<Map<String, Object>> response = controller.getStudentById(42L, loggedInRequest(9L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsEntry("studentId", 42L);
        assertThat(response.getData()).containsEntry("realName", "学生四二");
        assertThat(response.getData()).containsEntry("className", "未知班级");
        assertThat(response.getData()).containsEntry("averageScore", 91);
        verifyNoInteractions(userService);
    }

    @Test
    void updateStudentUsesUserServiceForProfileAndKeepsLocalPerformanceUpdate() {
        TeacherDashboardController controller = new TeacherDashboardController();
        CourseMapper courseMapper = mock(CourseMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserService userService = mock(UserService.class);
        UserServiceProfileClient userServiceProfileClient = mock(UserServiceProfileClient.class);

        ReflectionTestUtils.setField(controller, "teacherDashboardService", mock(TeacherDashboardService.class));
        ReflectionTestUtils.setField(controller, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", userServiceProfileClient);

        StudentProfileDTO updatedProfile = new StudentProfileDTO();
        updatedProfile.setStudentId(42L);
        updatedProfile.setRealName("新名字");
        updatedProfile.setEmail("new42@example.com");
        when(courseMapper.getStudentIdsByClassTeacherId(9L)).thenReturn(List.of(42L));
        when(userServiceProfileClient.updateStudentProfile(eq(42L), any(UpdateStudentProfileDTO.class)))
                .thenReturn(Optional.of(updatedProfile));

        ResponseResult<Object> response = controller.updateStudent(42L, Map.of(
                "realName", "新名字",
                "email", "new42@example.com",
                "averageScore", 95
        ), loggedInRequest(9L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isInstanceOf(StudentProfileDTO.class);
        assertThat(((StudentProfileDTO) response.getData()).getRealName()).isEqualTo("新名字");
        verify(userServiceProfileClient).updateStudentProfile(eq(42L), any(UpdateStudentProfileDTO.class));
        verify(studentMapper).updateStudentPerformance(42L, 95.0, null, null);
        verifyNoInteractions(userService);
    }

    @Test
    void addStudentToClass_allowsUnassignedStudentResolvedByUsername() {
        TeacherDashboardController controller = new TeacherDashboardController();
        CourseMapper courseMapper = mock(CourseMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserService userService = mock(UserService.class);

        ReflectionTestUtils.setField(controller, "teacherDashboardService", mock(TeacherDashboardService.class));
        ReflectionTestUtils.setField(controller, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", mock(UserServiceProfileClient.class));

        User student = new User();
        student.setId(88L);
        student.setUsername("20240088");
        student.setName("学生八八");

        when(courseMapper.countManagedClasses(9L, 5L)).thenReturn(1);
        when(userService.findByUsername("20240088")).thenReturn(student);
        when(userService.getRolesByUserId(88L)).thenReturn(List.of("STUDENT"));
        when(courseMapper.getClassesByStudentId(88L)).thenReturn(List.of());

        ResponseResult<Object> response = controller.addStudentToClass(
                5L,
                Map.of("studentIdentifier", "20240088"),
                loggedInRequest(9L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("学生已添加到班级");
        verify(studentMapper).insertStudentClass(5L, 88L);
    }

    @Test
    void addStudentToClass_rejectsStudentAlreadyAssignedToUnmanagedClass() {
        TeacherDashboardController controller = new TeacherDashboardController();
        CourseMapper courseMapper = mock(CourseMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserService userService = mock(UserService.class);

        ReflectionTestUtils.setField(controller, "teacherDashboardService", mock(TeacherDashboardService.class));
        ReflectionTestUtils.setField(controller, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", mock(UserServiceProfileClient.class));

        User student = new User();
        student.setId(99L);
        student.setUsername("20240099");
        student.setName("学生九九");

        when(courseMapper.countManagedClasses(9L, 5L)).thenReturn(1);
        when(userService.findByUsername("20240099")).thenReturn(student);
        when(userService.getRolesByUserId(99L)).thenReturn(List.of("STUDENT"));
        when(courseMapper.getClassesByStudentId(99L))
                .thenReturn(List.of(Map.of("id", 6L, "className", "其他班级")));
        when(courseMapper.countManagedClasses(9L, 6L)).thenReturn(0);

        ResponseResult<Object> response = controller.addStudentToClass(
                5L,
                Map.of("studentIdentifier", "20240099"),
                loggedInRequest(9L));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(403);
        assertThat(response.getMessage()).isEqualTo("无权移动该学生所在班级");
        verify(studentMapper, org.mockito.Mockito.never()).insertStudentClass(any(Long.class), any(Long.class));
    }

    @Test
    void getClassAssignments_clampsOutOfRangePageBeforeSlicingResults() {
        TeacherDashboardController controller = new TeacherDashboardController();
        CourseMapper courseMapper = mock(CourseMapper.class);

        ReflectionTestUtils.setField(controller, "teacherDashboardService", mock(TeacherDashboardService.class));
        ReflectionTestUtils.setField(controller, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "studentMapper", mock(StudentMapper.class));
        ReflectionTestUtils.setField(controller, "userService", mock(UserService.class));
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", mock(UserServiceProfileClient.class));

        when(courseMapper.countClassAssignments(9L, null, null)).thenReturn(3);
        when(courseMapper.getClassAssignments(9L, null, null)).thenReturn(List.of(
                Map.of("assignmentId", 1L, "classId", 101L, "courseId", 201L),
                Map.of("assignmentId", 2L, "classId", 102L, "courseId", 202L),
                Map.of("assignmentId", 3L, "classId", 103L, "courseId", 203L)
        ));

        ResponseResult<Map<String, Object>> response = controller.getClassAssignments(9, 2, null, null, loggedInRequest(9L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsEntry("totalElements", 3L);
        assertThat(response.getData()).containsEntry("totalPages", 2);
        assertThat(response.getData()).containsEntry("last", true);
        assertThat(response.getData()).containsEntry("first", false);
        assertThat(response.getData()).containsEntry("numberOfElements", 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getData().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0)).containsEntry("assignmentId", 3L);
    }

    @Test
    void getAllSubmissions_usesBoundedPaginationAndSpringPageResponseShape() {
        TeacherDashboardController controller = new TeacherDashboardController();
        AssignmentSubmissionService assignmentSubmissionService = mock(AssignmentSubmissionService.class);

        ReflectionTestUtils.setField(controller, "teacherDashboardService", mock(TeacherDashboardService.class));
        ReflectionTestUtils.setField(controller, "courseMapper", mock(CourseMapper.class));
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "studentMapper", mock(StudentMapper.class));
        ReflectionTestUtils.setField(controller, "userService", mock(UserService.class));
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", assignmentSubmissionService);
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", mock(UserServiceProfileClient.class));

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(77L);
        submission.setAssignmentId(8L);
        submission.setStudentId(6L);
        submission.setSubmissionDate(new Date());

        when(assignmentSubmissionService.countSubmissions(null, null, null)).thenReturn(3);
        when(assignmentSubmissionService.getSubmissionsWithPagination(2, 2, 3, "id", "DESC", null, null, null))
                .thenReturn(List.of(submission));

        ResponseResult<Map<String, Object>> response = controller.getAllSubmissions(
                9, 2, "id", "DESC", null, null, null, loggedInRequest(9L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsEntry("totalElements", 3L);
        assertThat(response.getData()).containsEntry("totalPages", 2);
        assertThat(response.getData()).containsEntry("number", 1);
        assertThat(response.getData()).containsEntry("size", 2);
        assertThat(response.getData()).containsEntry("last", true);
        assertThat(response.getData()).containsEntry("first", false);
        assertThat(response.getData().get("content"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(AssignmentSubmission.class))
                .hasSize(1)
                .first()
                .extracting(AssignmentSubmission::getId)
                .isEqualTo(77L);
        @SuppressWarnings("unchecked")
        Map<String, Object> pageable = (Map<String, Object>) response.getData().get("pageable");
        assertThat(pageable)
                .containsEntry("pageNumber", 1)
                .containsEntry("pageSize", 2)
                .containsEntry("offset", 2);

        verify(assignmentSubmissionService)
                .getSubmissionsWithPagination(2, 2, 3, "id", "DESC", null, null, null);
    }

    @Test
    void getScoreTrend_forwardsStudentIdToService() {
        TeacherDashboardController controller = new TeacherDashboardController();
        TeacherDashboardService teacherDashboardService = mock(TeacherDashboardService.class);

        ReflectionTestUtils.setField(controller, "teacherDashboardService", teacherDashboardService);
        ReflectionTestUtils.setField(controller, "courseMapper", mock(CourseMapper.class));
        ReflectionTestUtils.setField(controller, "courseService", mock(CourseService.class));
        ReflectionTestUtils.setField(controller, "studentMapper", mock(StudentMapper.class));
        ReflectionTestUtils.setField(controller, "userService", mock(UserService.class));
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", mock(UserServiceProfileClient.class));

        when(teacherDashboardService.getScoreTrend(9L, 3L, 11L, 42L, "week")).thenReturn(List.of());

        ResponseResult<?> response = controller.getScoreTrend("3", "11", 42L, "week", loggedInRequest(9L));

        assertThat(response.isSuccess()).isTrue();
        verify(teacherDashboardService).getScoreTrend(9L, 3L, 11L, 42L, "week");
    }

    private static MockHttpServletRequest loggedInRequest(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                MultiRoleSessionFilter.CURRENT_USER_ATTR,
                MultiRoleSessionFilter.AuthUser.of(userId, List.of("TEACHER")));
        return request;
    }
}
