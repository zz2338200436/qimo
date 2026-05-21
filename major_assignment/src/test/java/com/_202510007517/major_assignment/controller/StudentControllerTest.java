package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.entity.Assignment;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.Exam;
import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.AssignmentService;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.ExamService;
import com._202510007517.major_assignment.service.ExamSubmissionService;
import com._202510007517.major_assignment.service.StudentService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StudentControllerTest {

    @Test
    void getAssignmentDetailUsesUserServiceForTeacherProfile() {
        StudentController controller = new StudentController();
        AssignmentService assignmentService = mock(AssignmentService.class);
        CourseService courseService = mock(CourseService.class);
        UserService userService = mock(UserService.class);
        UserServiceProfileClient userServiceProfileClient = mock(UserServiceProfileClient.class);

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "studentService", mock(StudentService.class));
        ReflectionTestUtils.setField(controller, "courseService", courseService);
        ReflectionTestUtils.setField(controller, "assignmentService", assignmentService);
        ReflectionTestUtils.setField(controller, "examService", mock(ExamService.class));
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "examSubmissionService", mock(ExamSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", userServiceProfileClient);

        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setTitle("作业 A");
        assignment.setDescription("desc");
        assignment.setCourseId(5L);
        assignment.setTeacherId(7L);
        assignment.setDueDate(new Date());
        assignment.setPublishDate(new Date());
        assignment.setIsActive(true);
        Course course = new Course();
        course.setId(5L);
        course.setCourseName("数学");
        UserProfileDTO teacher = new UserProfileDTO();
        teacher.setId(7L);
        teacher.setName("王老师");
        when(assignmentService.getAssignmentById(10L)).thenReturn(assignment);
        when(courseService.findById(5L)).thenReturn(course);
        when(userServiceProfileClient.getUserProfile(7L)).thenReturn(Optional.of(teacher));

        ResponseResult<Map<String, Object>> response = controller.getAssignmentDetail(10L, loggedInSession(42L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsEntry("teacherId", 7L);
        assertThat(response.getData()).containsEntry("teacherName", "王老师");
        verifyNoInteractions(userService);
    }

    @Test
    void getExamDetailUsesUserServiceForTeacherProfile() {
        StudentController controller = new StudentController();
        ExamService examService = mock(ExamService.class);
        CourseService courseService = mock(CourseService.class);
        UserService userService = mock(UserService.class);
        UserServiceProfileClient userServiceProfileClient = mock(UserServiceProfileClient.class);

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "studentService", mock(StudentService.class));
        ReflectionTestUtils.setField(controller, "courseService", courseService);
        ReflectionTestUtils.setField(controller, "assignmentService", mock(AssignmentService.class));
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "examSubmissionService", mock(ExamSubmissionService.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", userServiceProfileClient);

        Exam exam = new Exam();
        exam.setId(12L);
        exam.setTitle("考试 B");
        exam.setDescription("desc");
        exam.setCourseId(5L);
        exam.setTeacherId(8L);
        exam.setStartTime(new Date());
        exam.setEndTime(new Date());
        exam.setPublishDate(new Date());
        exam.setDuration(90L);
        exam.setIsActive(true);
        Course course = new Course();
        course.setId(5L);
        course.setCourseName("英语");
        UserProfileDTO teacher = new UserProfileDTO();
        teacher.setId(8L);
        teacher.setName("李老师");
        when(examService.getExamById(12L)).thenReturn(exam);
        when(courseService.findById(5L)).thenReturn(course);
        when(userServiceProfileClient.getUserProfile(8L)).thenReturn(Optional.of(teacher));

        ResponseResult<Map<String, Object>> response = controller.getExamDetail(12L, loggedInSession(42L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsEntry("teacherId", 8L);
        assertThat(response.getData()).containsEntry("teacherName", "李老师");
        verifyNoInteractions(userService);
    }

    @Test
    void getExamDetailIncludesSubmissionContent() {
        StudentController controller = new StudentController();
        ExamService examService = mock(ExamService.class);
        CourseService courseService = mock(CourseService.class);
        ExamSubmissionService examSubmissionService = mock(ExamSubmissionService.class);

        ReflectionTestUtils.setField(controller, "userService", mock(UserService.class));
        ReflectionTestUtils.setField(controller, "studentService", mock(StudentService.class));
        ReflectionTestUtils.setField(controller, "courseService", courseService);
        ReflectionTestUtils.setField(controller, "assignmentService", mock(AssignmentService.class));
        ReflectionTestUtils.setField(controller, "examService", examService);
        ReflectionTestUtils.setField(controller, "assignmentSubmissionService", mock(AssignmentSubmissionService.class));
        ReflectionTestUtils.setField(controller, "examSubmissionService", examSubmissionService);
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", mock(UserServiceProfileClient.class));

        Exam exam = new Exam();
        exam.setId(9001L);
        exam.setTitle("考试 C");
        exam.setDescription("desc");
        exam.setCourseId(5L);
        exam.setTeacherId(8L);
        exam.setStartTime(new Date());
        exam.setEndTime(new Date());
        exam.setPublishDate(new Date());
        exam.setDuration(90L);
        exam.setIsActive(true);

        Course course = new Course();
        course.setId(5L);
        course.setCourseName("英语");

        ExamSubmission submission = new ExamSubmission();
        submission.setId(123L);
        submission.setSubmissionDate(new Date());
        submission.setTimeTaken(10);
        submission.setScore(0);
        submission.setTeacherComment(null);
        submission.setGraded(false);
        submission.setContent("{\"content\":\"student exam answer\"}");

        when(examService.getExamById(9001L)).thenReturn(exam);
        when(courseService.findById(5L)).thenReturn(course);
        when(examSubmissionService.getSubmissionByExamAndStudent(9001L, 42L)).thenReturn(submission);

        ResponseResult<Map<String, Object>> response = controller.getExamDetail(9001L, loggedInSession(42L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsKey("submission");
        @SuppressWarnings("unchecked")
        Map<String, Object> submissionMap = (Map<String, Object>) response.getData().get("submission");
        assertThat(submissionMap).containsEntry("content", "{\"content\":\"student exam answer\"}");
    }

    private static HttpSession loggedInSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }
}
