package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PendingAssignmentQueryToolTest {

    private final AssignmentFeignClient assignmentClient = mock(AssignmentFeignClient.class);
    private final PendingAssignmentQueryTool tool = new PendingAssignmentQueryTool(assignmentClient);

    @Test
    void reportsPendingAssignmentQueryIntent() {
        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_PENDING_ASSIGNMENTS);
    }

    @Test
    void queriesStudentVisibleUnsubmittedAssignments() {
        Map<String, Object> page = Map.of(
                "content", List.of(Map.of("title", "待提交实验", "submitted", false)),
                "totalElements", 1);
        when(assignmentClient.listStudentAssignments(7L, 1, 10, "dueDate", "DESC", null, false, true))
                .thenReturn(page);

        Map<String, Object> result = tool.execute(7L, "STUDENT", Map.of());

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result).containsEntry("pendingAssignments", page);
        verify(assignmentClient).listStudentAssignments(7L, 1, 10, "dueDate", "DESC", null, false, true);
    }
}
