package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.AgentServiceApplication;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import com._202510007517.platform.agent.repository.AgentSessionRepository;
import com._202510007517.platform.agent.service.AgentOrchestrator;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = AgentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:agent-readonly-tools;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        })
class ReadOnlyToolTest {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private AgentActionRepository actionRepository;

    @Autowired
    private AgentAuditLogRepository auditLogRepository;

    @Autowired
    private AgentSessionRepository sessionRepository;

    @MockitoBean
    private CourseFeignClient courseFeignClient;

    @MockitoBean
    private AssignmentFeignClient assignmentFeignClient;

    @MockitoBean
    private ExamFeignClient examFeignClient;

    @BeforeEach
    void clearData() {
        auditLogRepository.deleteAll();
        actionRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void readOnlyCourseQueryExecutesToolWithoutCreatingPendingAction() {
        CourseDTO course = new CourseDTO();
        course.setCourseName("Java 分布式框架");
        when(courseFeignClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查看我的课程");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        assertThat(response.getData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsKey("courses");
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void readOnlyAssignmentQueryExecutesToolWithoutCreatingPendingAction() {
        AssignmentStudentScoreDTO score = new AssignmentStudentScoreDTO();
        score.setTitle("数据库实验");
        when(assignmentFeignClient.listStudentScores(7L)).thenReturn(List.of(score));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "查看我的作业列表");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsKey("assignments");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void pendingAssignmentQueryExecutesToolWithoutCreatingPendingAction() {
        Map<String, Object> page = Map.of(
                "content", List.of(Map.of("title", "数据库实验", "submitted", false)),
                "totalElements", 1);
        when(assignmentFeignClient.listStudentAssignments(7L, 1, 10, "dueDate", "DESC", null, false, true))
                .thenReturn(page);

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "我有哪些待提交作业");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("pendingAssignments", page);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void readOnlyExamQueryExecutesToolWithoutCreatingPendingAction() {
        ExamDTO exam = new ExamDTO();
        exam.setTitle("期末考试");
        when(examFeignClient.listByStudent(7L)).thenReturn(List.of(exam));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "查看考试列表");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsKey("exams");
        assertThat(actionRepository.findAll()).isEmpty();
    }
}
