package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAnalysisEdgeClient;
import com._202510007517.platform.agent.client.TeacherKnowledgeAnalysisEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeMasteryQueryTool implements AgentTool {

    private final TeacherAnalysisEdgeClient analysisClient;
    private final TeacherKnowledgeAnalysisEdgeClient knowledgeAnalysisClient;

    public KnowledgeMasteryQueryTool(TeacherAnalysisEdgeClient analysisClient,
                                     TeacherKnowledgeAnalysisEdgeClient knowledgeAnalysisClient) {
        this.analysisClient = analysisClient;
        this.knowledgeAnalysisClient = knowledgeAnalysisClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_KNOWLEDGE_MASTERY;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Long studentId = asLong(request.get("studentId"));
        Long courseId = asLong(request.get("courseId"));
        if (studentId == null) {
            ResponseResult<Map<String, Object>> response = knowledgeAnalysisClient.getKnowledgePointAnalysis(
                    String.valueOf(userId),
                    courseId,
                    asLong(request.get("classId")),
                    null,
                    asLong(request.get("knowledgePointId")));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
            result.put("knowledgeAnalysis", response.getData());
            result.put("message", response.getMessage());
            return result;
        }
        if (courseId == null) {
            return Map.of("status", "VALIDATION_FAILED", "message", "缺少课程ID。");
        }
        ResponseResult<List<KnowledgeMasteryDTO>> response = analysisClient.listStudentKnowledgeMastery(
                String.valueOf(userId),
                studentId,
                courseId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("knowledgeMastery", response.getData());
        result.put("message", response.getMessage());
        return result;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank() && !"all".equalsIgnoreCase(text)) {
            return Long.valueOf(text);
        }
        return null;
    }
}
