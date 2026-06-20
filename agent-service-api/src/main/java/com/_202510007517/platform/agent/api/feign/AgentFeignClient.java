package com._202510007517.platform.agent.api.feign;

import com._202510007517.platform.agent.api.dto.AgentActionConfirmDTO;
import com._202510007517.platform.agent.api.dto.AgentActionDTO;
import com._202510007517.platform.agent.api.dto.AgentChatRequestDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "agent-service", path = "/api/agent")
public interface AgentFeignClient {
    @PostMapping("/chat")
    ResponseResult<AgentChatResponseDTO> chat(@RequestBody @Valid AgentChatRequestDTO request);

    @PostMapping("/actions/{actionId}/confirm")
    ResponseResult<AgentExecutionResultDTO> confirm(@PathVariable("actionId") Long actionId,
                                                    @RequestBody @Valid AgentActionConfirmDTO request);

    @PostMapping("/actions/{actionId}/cancel")
    ResponseResult<AgentExecutionResultDTO> cancel(@PathVariable("actionId") Long actionId);

    @GetMapping("/actions/{actionId}")
    ResponseResult<AgentActionDTO> getAction(@PathVariable("actionId") Long actionId);

    @GetMapping("/sessions")
    ResponseResult<List<AgentSessionDTO>> listSessions();

    @GetMapping("/sessions/{sessionId}")
    ResponseResult<AgentSessionDTO> getSession(@PathVariable("sessionId") Long sessionId);
}
