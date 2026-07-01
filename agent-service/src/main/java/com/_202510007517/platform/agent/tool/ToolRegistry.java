package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<AgentIntent, AgentTool> tools = new EnumMap<>(AgentIntent.class);

    public ToolRegistry(List<AgentTool> tools) {
        for (AgentTool tool : tools) {
            this.tools.put(tool.intent(), tool);
        }
    }

    public AgentTool resolve(AgentIntent intent) {
        AgentTool tool = tools.get(intent);
        if (tool == null) {
            throw new IllegalArgumentException("No agent tool registered for intent: " + intent);
        }
        return tool;
    }
}
