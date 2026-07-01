package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PendingAssignmentQueryTool implements AgentTool {

    private final AssignmentFeignClient assignmentClient;

    public PendingAssignmentQueryTool(AssignmentFeignClient assignmentClient) {
        this.assignmentClient = assignmentClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_PENDING_ASSIGNMENTS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("pendingAssignments", assignmentClient.listStudentAssignments(
                userId, 1, 10, "dueDate", "DESC", null, false, true));
        return result;
    }
}
