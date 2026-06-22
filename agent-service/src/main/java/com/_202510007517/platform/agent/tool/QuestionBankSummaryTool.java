package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.questionbank.QuestionBankSummaryService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QuestionBankSummaryTool implements AgentTool {

    private final QuestionBankSummaryService summaryService;

    public QuestionBankSummaryTool(QuestionBankSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_QUESTION_BANK;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("questionBank", summaryService.summary());
        result.put("message", "题库查询完成。");
        return result;
    }
}
