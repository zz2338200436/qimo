package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ExamQueryTool implements AgentTool {

    private final ExamFeignClient examClient;

    public ExamQueryTool(ExamFeignClient examClient) {
        this.examClient = examClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_EXAMS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("exams", examClient.listByStudent(userId));
        return result;
    }
}
