package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.TeacherDashboardService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class TeacherDashboardControllerTest {

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

        ResponseResult<Map<String, Object>> response = controller.getStudentById(42L, loggedInSession(9L));

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
        ), loggedInSession(9L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isInstanceOf(StudentProfileDTO.class);
        assertThat(((StudentProfileDTO) response.getData()).getRealName()).isEqualTo("新名字");
        verify(userServiceProfileClient).updateStudentProfile(eq(42L), any(UpdateStudentProfileDTO.class));
        verify(studentMapper).updateStudentPerformance(42L, 95.0, null, null);
        verifyNoInteractions(userService);
    }

    private static HttpSession loggedInSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }
}
