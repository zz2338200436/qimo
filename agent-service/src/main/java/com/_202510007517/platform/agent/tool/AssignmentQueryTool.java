package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AssignmentQueryTool implements AgentTool {

    private final AssignmentFeignClient assignmentClient;

    public AssignmentQueryTool(AssignmentFeignClient assignmentClient) {
        this.assignmentClient = assignmentClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_ASSIGNMENTS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("assignments", assignmentClient.listStudentScores(userId));
        return result;
    }
}
