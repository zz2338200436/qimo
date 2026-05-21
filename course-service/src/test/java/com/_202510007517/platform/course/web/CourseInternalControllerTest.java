package com._202510007517.platform.course.web;

import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentRequestDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseInternalControllerTest {

    @Test
    void listTeacherCoursesBindsFilters() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.listTeacherCourses(7L, "分布式", "DF101", "必修", "active"))
                .thenReturn(List.of(course(101L)));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses")
                        .param("teacherId", "7")
                        .param("courseName", "分布式")
                        .param("courseCode", "DF101")
                        .param("category", "必修")
                        .param("courseStatus", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].courseName").value("分布式框架技术"));

        verify(service).listTeacherCourses(7L, "分布式", "DF101", "必修", "active");
    }

    @Test
    void getCourseReturnsInternalDto() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.getCourse(101L)).thenReturn(course(101L));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.courseCode").value("DF101"));

        verify(service).getCourse(101L);
    }

    @Test
    void createCourseBindsTeacherAndBody() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        ArgumentCaptor<CourseUpsertRequestDTO> requestCaptor = ArgumentCaptor.forClass(CourseUpsertRequestDTO.class);
        when(service.createCourse(eq(7L), any(CourseUpsertRequestDTO.class))).thenReturn(course(101L));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/internal/courses")
                        .param("teacherId", "7")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"courseName":"分布式框架技术","courseCode":"DF101","description":"Spring Cloud",
                                "credit":3,"courseCategory":"必修","totalHours":48,"courseDirector":7,
                                "assessmentMethod":"考试","courseStatus":"active","semester":"2026-2027-1",
                                "startDate":"2026-09-01","endDate":"2027-01-10","maxStudents":80}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101));

        verify(service).createCourse(eq(7L), requestCaptor.capture());
        CourseUpsertRequestDTO request = requestCaptor.getValue();
        assertThat(request.getCourseName()).isEqualTo("分布式框架技术");
        assertThat(request.getCourseCode()).isEqualTo("DF101");
        assertThat(request.getCredit()).isEqualTo(3);
        assertThat(request.getTotalHours()).isEqualTo(48);
        assertThat(request.getStartDate()).isEqualTo("2026-09-01");
    }

    @Test
    void updateCourseBindsCourseTeacherAndBody() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        ArgumentCaptor<CourseUpsertRequestDTO> requestCaptor = ArgumentCaptor.forClass(CourseUpsertRequestDTO.class);
        when(service.updateCourse(eq(101L), eq(7L), any(CourseUpsertRequestDTO.class))).thenReturn(course(101L));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(put("/internal/courses/101")
                        .param("teacherId", "7")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"courseName":"分布式框架技术","courseCode":"DF101","credit":4,"totalHours":64}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101));

        verify(service).updateCourse(eq(101L), eq(7L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getCredit()).isEqualTo(4);
        assertThat(requestCaptor.getValue().getTotalHours()).isEqualTo(64);
    }

    @Test
    void deleteCourseDelegatesToService() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(delete("/internal/courses/101"))
                .andExpect(status().isOk());

        verify(service).deleteCourse(101L);
    }

    @Test
    void listCourseStudentsReturnsRows() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.listCourseStudents(101L)).thenReturn(List.of(Map.of(
                "id", 42L,
                "username", "student42",
                "name", "学生四二"
        )));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses/101/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].username").value("student42"));

        verify(service).listCourseStudents(101L);
    }

    @Test
    void listStudentClassIdsReturnsArray() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.listStudentClassIds(42L)).thenReturn(List.of(301L, 302L));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses/students/42/class-ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(301))
                .andExpect(jsonPath("$[1]").value(302));

        verify(service).listStudentClassIds(42L);
    }

    @Test
    void listTeacherClassesBindsFilters() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        TeacherClassDTO dto = new TeacherClassDTO();
        dto.setId(301L);
        dto.setClassName("软件工程一班");
        dto.setYear("2026");
        dto.setMajorName("软件工程");
        when(service.listTeacherClasses(7L, "软件", "2026", "软件工程", 8L, 101L)).thenReturn(List.of(dto));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses/classes")
                        .param("teacherId", "7")
                        .param("className", "软件")
                        .param("grade", "2026")
                        .param("majorName", "软件工程")
                        .param("majorId", "8")
                        .param("courseId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(301))
                .andExpect(jsonPath("$[0].className").value("软件工程一班"));

        verify(service).listTeacherClasses(7L, "软件", "2026", "软件工程", 8L, 101L);
    }

    @Test
    void getClassBindsTeacherAndClass() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.getClass(7L, 301L)).thenReturn(Map.of(
                "id", 301L,
                "className", "软件工程一班"
        ));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses/classes/301")
                        .param("teacherId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(301))
                .andExpect(jsonPath("$.className").value("软件工程一班"));

        verify(service).getClass(7L, 301L);
    }

    @Test
    void createClassBindsTeacherAndBody() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        ArgumentCaptor<ClassUpsertRequestDTO> requestCaptor = ArgumentCaptor.forClass(ClassUpsertRequestDTO.class);
        when(service.createClass(eq(7L), any(ClassUpsertRequestDTO.class))).thenReturn(301L);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/internal/courses/classes")
                        .param("teacherId", "7")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"className":"软件工程一班","year":"2026","capacity":45,"courseId":101,
                                "majorId":8,"classTime":"周一 1-2 节","classLocation":"A101"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("301"));

        verify(service).createClass(eq(7L), requestCaptor.capture());
        ClassUpsertRequestDTO request = requestCaptor.getValue();
        assertThat(request.getClassName()).isEqualTo("软件工程一班");
        assertThat(request.getCapacity()).isEqualTo(45);
        assertThat(request.getCourseId()).isEqualTo(101L);
        assertThat(request.getMajorId()).isEqualTo(8L);
    }

    @Test
    void updateClassBindsTeacherClassAndBody() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        ArgumentCaptor<ClassUpsertRequestDTO> requestCaptor = ArgumentCaptor.forClass(ClassUpsertRequestDTO.class);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(put("/internal/courses/classes/301")
                        .param("teacherId", "7")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"className":"软件工程二班","year":"2026","capacity":50,"courseId":102}
                                """))
                .andExpect(status().isOk());

        verify(service).updateClass(eq(7L), eq(301L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getClassName()).isEqualTo("软件工程二班");
        assertThat(requestCaptor.getValue().getCourseId()).isEqualTo(102L);
    }

    @Test
    void deleteClassDelegatesToService() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(delete("/internal/courses/classes/301")
                        .param("teacherId", "7"))
                .andExpect(status().isOk());

        verify(service).deleteClass(7L, 301L);
    }

    @Test
    void listCourseAssignmentsBindsFilters() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        CourseAssignmentDTO dto = new CourseAssignmentDTO();
        dto.setAssignmentId(9001L);
        dto.setClassId(301L);
        dto.setCourseId(101L);
        dto.setCourseName("分布式框架技术");
        when(service.listCourseAssignments(7L, 101L, 301L)).thenReturn(List.of(dto));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses/course-assignments")
                        .param("teacherId", "7")
                        .param("courseId", "101")
                        .param("classId", "301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentId").value(9001))
                .andExpect(jsonPath("$[0].courseName").value("分布式框架技术"));

        verify(service).listCourseAssignments(7L, 101L, 301L);
    }

    @Test
    void assignCourseBindsTeacherAndBody() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        ArgumentCaptor<CourseAssignmentRequestDTO> requestCaptor = ArgumentCaptor.forClass(CourseAssignmentRequestDTO.class);
        when(service.assignCourse(eq(7L), any(CourseAssignmentRequestDTO.class))).thenReturn(9001L);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/internal/courses/course-assignments")
                        .param("teacherId", "7")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"classId":301,"courseId":101,"classTime":"周一 1-2 节","classLocation":"A101"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("9001"));

        verify(service).assignCourse(eq(7L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getClassId()).isEqualTo(301L);
        assertThat(requestCaptor.getValue().getCourseId()).isEqualTo(101L);
        assertThat(requestCaptor.getValue().getClassLocation()).isEqualTo("A101");
    }

    @Test
    void unassignCourseDelegatesToService() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(delete("/internal/courses/course-assignments/9001")
                        .param("teacherId", "7"))
                .andExpect(status().isOk());

        verify(service).unassignCourse(7L, 9001L);
    }

    @Test
    void unassignClassCourseBindsQueryParameters() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(delete("/internal/courses/class-courses/unassign")
                        .param("teacherId", "7")
                        .param("classId", "301")
                        .param("courseId", "101"))
                .andExpect(status().isOk());

        verify(service).unassignClassCourse(7L, 301L, 101L);
    }

    @Test
    void listMajorsReturnsInternalArray() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        MajorDTO dto = new MajorDTO();
        dto.setId(8L);
        dto.setMajorName("软件工程");
        when(service.listMajors()).thenReturn(List.of(dto));
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get("/internal/courses/majors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].majorName").value("软件工程"));

        verify(service).listMajors();
    }

    private static MockMvc mockMvc(CourseApplicationService service) {
        return MockMvcBuilders.standaloneSetup(new CourseInternalController(service)).build();
    }

    private static CourseDTO course(Long id) {
        CourseDTO dto = new CourseDTO();
        dto.setId(id);
        dto.setCourseName("分布式框架技术");
        dto.setCourseCode("DF101");
        dto.setCredit(3);
        dto.setCourseCategory("必修");
        dto.setTotalHours(48);
        dto.setTeacherId(7L);
        dto.setCourseStatus("active");
        return dto;
    }
}
