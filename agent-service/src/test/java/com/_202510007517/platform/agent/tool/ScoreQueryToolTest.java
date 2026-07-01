package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.StudentExamEdgeClient;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.StudentScoreListItemDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreQueryToolTest {

    private final StudentExamEdgeClient examClient = mock(StudentExamEdgeClient.class);
    private final ScoreQueryTool tool = new ScoreQueryTool(examClient);

    @Test
    void listsScoresThroughExamService() {
        StudentScoreListItemDTO score = new StudentScoreListItemDTO();
        score.setId(7001L);
        score.setType("exam");
        score.setCourseName("分布式框架技术");
        score.setTitle("期末考试");
        score.setScore(88);
        score.setTotalScore(100);
        when(examClient.listScores("42")).thenReturn(ResponseResult.success(List.of(score)));

        Map<String, Object> result = tool.execute(42L, "STUDENT", Map.of());

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("scores")).isEqualTo(List.of(score));
        verify(examClient).listScores("42");
    }
}
