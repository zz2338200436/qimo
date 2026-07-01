package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;

import java.util.Map;

public interface AgentTool {
    AgentIntent intent();

    Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request);
}
