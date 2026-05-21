package com._202510007517.platform.course.web;

import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.service.TeacherKnowledgePointService;
import com._202510007517.platform.course.service.CourseApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseControllerTest {

    @Test
    void listTeacherCoursesWrapsPageResult() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        CourseDTO course = new CourseDTO();
        course.setId(101L);
        course.setCourseName("Distributed Systems");
        course.setCourseCode("DS101");
        course.setStudentCount(36);
        when(service.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(service)).build();

        mockMvc.perform(get("/api/teacher/courses")
                        .param("teacherId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(101))
                .andExpect(jsonPath("$.data.content[0].courseName").value("Distributed Systems"))
                .andExpect(jsonPath("$.data.content[0].studentCount").value(36))
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listTeacherClassesUsesGatewayUserHeader() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        TeacherClassDTO cls = new TeacherClassDTO();
        cls.setId(501L);
        cls.setClassName("软件 2301");
        cls.setCourseName("Distributed Systems");
        when(service.listTeacherClasses(7L, null, null, null, null, null)).thenReturn(List.of(cls));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherCourseAdminController(service)).build();

        mockMvc.perform(get("/api/teacher/classes")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(501))
                .andExpect(jsonPath("$.data[0].className").value("软件 2301"));
    }

    @Test
    void listCourseAssignmentsWrapsPageResult() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        CourseAssignmentDTO assignment = new CourseAssignmentDTO();
        assignment.setAssignmentId(9001L);
        assignment.setClassId(501L);
        assignment.setCourseId(101L);
        assignment.setCourseName("Distributed Systems");
        when(service.listCourseAssignments(7L, null, null)).thenReturn(List.of(assignment));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherCourseAdminController(service)).build();

        mockMvc.perform(get("/api/teacher/course-assignments")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].assignmentId").value(9001))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getClassStudentsUsesGatewayUserHeader() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.listClassStudents(7L, 501L)).thenReturn(List.of(
                java.util.Map.of(
                        "id", 42L,
                        "username", "student42",
                        "name", "Student Forty Two",
                        "email", "student42@example.com",
                        "phone", "13800000042"
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherCourseAdminController(service)).build();

        mockMvc.perform(get("/api/teacher/classes/{classId}/students", 501L)
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(42))
                .andExpect(jsonPath("$.data[0].username").value("student42"));
    }

    @Test
    void checkClassNameKeepsLegacyTeacherEnvelope() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.classNameExists("软件 2301", 501L)).thenReturn(true);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherCourseAdminController(service)).build();

        mockMvc.perform(get("/api/teacher/check-class-name")
                        .header("X-User-Id", "7")
                        .param("className", "软件 2301")
                        .param("classId", "501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("班级名称检查成功"))
                .andExpect(jsonPath("$.data.exists").value(true));

        verify(service).classNameExists("软件 2301", 501L);
    }

    @Test
    void getTeacherStudentKeepsLegacyEnvelope() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.getTeacherStudent(7L, 42L)).thenReturn(Map.of(
                "studentId", 42L,
                "realName", "Student Forty Two",
                "username", "student42",
                "className", "软件 2301",
                "averageScore", 86.0,
                "overallProgress", 76));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherCourseAdminController(service)).build();

        mockMvc.perform(get("/api/teacher/students/{studentId}", 42L)
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学生详情成功"))
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.realName").value("Student Forty Two"))
                .andExpect(jsonPath("$.data.className").value("软件 2301"));

        verify(service).getTeacherStudent(7L, 42L);
    }

    @Test
    void updateTeacherStudentKeepsLegacyEnvelope() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        Map<String, Object> request = Map.of(
                "realName", "Student Renamed",
                "email", "student42@example.com",
                "classId", 501);
        when(service.updateTeacherStudent(7L, 42L, request)).thenReturn(Map.of(
                "studentId", 42L,
                "realName", "Student Renamed",
                "email", "student42@example.com",
                "className", "软件 2301"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherCourseAdminController(service)).build();

        mockMvc.perform(put("/api/teacher/students/{studentId}", 42L)
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "realName":"Student Renamed",
                                  "email":"student42@example.com",
                                  "classId":501
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("学生信息更新成功"))
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.realName").value("Student Renamed"));

        verify(service).updateTeacherStudent(7L, 42L, request);
    }

    @Test
    void listStudentCoursesUsesGatewayUserHeader() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        CourseDTO course = new CourseDTO();
        course.setId(201L);
        course.setCourseName("Operating Systems");
        course.setCourseCode("OS201");
        course.setTeacherName("Teacher Seven");
        course.setStudentCount(42);
        when(service.listStudentCourses(42L, null, null, null)).thenReturn(List.of(course));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentCourseController(service)).build();

        mockMvc.perform(get("/api/student/courses")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(201))
                .andExpect(jsonPath("$.data.content[0].courseName").value("Operating Systems"))
                .andExpect(jsonPath("$.data.content[0].teacherName").value("Teacher Seven"));
    }

    @Test
    void getStudentCourseUsesGatewayUserHeader() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        CourseDTO course = new CourseDTO();
        course.setId(201L);
        course.setCourseName("Operating Systems");
        course.setCourseCode("OS201");
        when(service.getStudentCourse(42L, 201L)).thenReturn(course);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentCourseController(service)).build();

        mockMvc.perform(get("/api/student/courses/{courseId}", 201L)
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(201))
                .andExpect(jsonPath("$.data.courseCode").value("OS201"));
    }

    @Test
    void listTeacherKnowledgePointsUsesGatewayUserHeader() throws Exception {
        TeacherKnowledgePointService service = mock(TeacherKnowledgePointService.class);
        when(service.listKnowledgePoints(7L, null)).thenReturn(List.of(
                Map.of(
                        "id", 11L,
                        "pointName", "面向对象基础",
                        "courseId", 101L,
                        "difficulty", "中等",
                        "orderIndex", 1
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(get("/api/teacher/knowledge-points")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取知识点列表成功"))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].pointName").value("面向对象基础"));

        verify(service).listKnowledgePoints(7L, null);
    }

    @Test
    void getTeacherKnowledgePointsByCourseUsesLegacyUrl() throws Exception {
        TeacherKnowledgePointService service = mock(TeacherKnowledgePointService.class);
        when(service.listKnowledgePointsByCourse(7L, 101L)).thenReturn(List.of(
                Map.of(
                        "id", 12L,
                        "pointName", "分布式一致性",
                        "courseId", 101L,
                        "difficulty", "困难",
                        "orderIndex", 2
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(get("/api/teacher/knowledge-points/course/101")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].pointName").value("分布式一致性"));

        verify(service).listKnowledgePointsByCourse(7L, 101L);
    }

    @Test
    void getTeacherKnowledgePointByIdUsesLegacyEnvelope() throws Exception {
        TeacherKnowledgePointService service = mock(TeacherKnowledgePointService.class);
        when(service.getKnowledgePoint(7L, 11L)).thenReturn(Map.of(
                "id", 11L,
                "pointName", "面向对象基础",
                "description", "封装、继承、多态",
                "difficulty", "中等",
                "orderIndex", 1,
                "courseId", 101L
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(get("/api/teacher/knowledge-points/11")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取知识点成功"))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.description").value("封装、继承、多态"));

        verify(service).getKnowledgePoint(7L, 11L);
    }

    @Test
    void createTeacherKnowledgePointReturnsLegacyCreatedEnvelope() throws Exception {
        TeacherKnowledgePointService service = mock(TeacherKnowledgePointService.class);
        when(service.createKnowledgePoint(7L, Map.of(
                "pointName", "面向对象基础",
                "description", "封装、继承、多态",
                "difficulty", "中等",
                "orderIndex", 1,
                "courseId", 101
        ))).thenReturn(Map.of(
                "id", 11L,
                "pointName", "面向对象基础",
                "description", "封装、继承、多态",
                "difficulty", "中等",
                "orderIndex", 1,
                "courseId", 101L
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(post("/api/teacher/knowledge-points")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pointName":"面向对象基础",
                                  "description":"封装、继承、多态",
                                  "difficulty":"中等",
                                  "orderIndex":1,
                                  "courseId":101
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("创建知识点成功"))
                .andExpect(jsonPath("$.data.id").value(11));
    }

    @Test
    void updateTeacherKnowledgePointReturnsLegacyEnvelope() throws Exception {
        TeacherKnowledgePointService service = mock(TeacherKnowledgePointService.class);
        when(service.updateKnowledgePoint(7L, 11L, Map.of(
                "pointName", "面向对象进阶",
                "description", "抽象类与接口",
                "difficulty", "中等",
                "orderIndex", 2,
                "courseId", 101
        ))).thenReturn(Map.of(
                "id", 11L,
                "pointName", "面向对象进阶",
                "description", "抽象类与接口",
                "difficulty", "中等",
                "orderIndex", 2,
                "courseId", 101L
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(put("/api/teacher/knowledge-points/11")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pointName":"面向对象进阶",
                                  "description":"抽象类与接口",
                                  "difficulty":"中等",
                                  "orderIndex":2,
                                  "courseId":101
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新知识点成功"))
                .andExpect(jsonPath("$.data.pointName").value("面向对象进阶"));
    }

    @Test
    void deleteTeacherKnowledgePointReturnsLegacyEnvelope() throws Exception {
        TeacherKnowledgePointService service = mock(TeacherKnowledgePointService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(delete("/api/teacher/knowledge-points/11")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除知识点成功"));

        verify(service).deleteKnowledgePoint(7L, 11L);
    }
}
