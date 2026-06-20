package com._202510007517.platform.agent.controller;

import com._202510007517.platform.agent.api.dto.AgentActionConfirmDTO;
import com._202510007517.platform.agent.api.dto.AgentActionDTO;
import com._202510007517.platform.agent.api.dto.AgentChatRequestDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.agent.service.AgentOrchestrator;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentOrchestrator orchestrator;

    public AgentController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public ResponseResult<AgentChatResponseDTO> chat(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRoleHeader,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String rolesHeader,
            @RequestBody @Valid AgentChatRequestDTO request) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        String role = resolveRole(activeRoleHeader, rolesHeader);
        return ResponseResult.success(orchestrator.chat(userId, role, request.getSessionId(), request.getMessage()));
    }

    @PostMapping("/actions/{actionId}/confirm")
    public ResponseResult<AgentExecutionResultDTO> confirm(
            @PathVariable("actionId") Long actionId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRoleHeader,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String rolesHeader,
            @RequestBody @Valid AgentActionConfirmDTO request) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        String role = resolveRole(activeRoleHeader, rolesHeader);
        return ResponseResult.success(orchestrator.confirm(
                userId,
                role,
                actionId,
                request.getIdempotencyKey(),
                request.getSecondConfirmationText()));
    }

    @PostMapping("/actions/{actionId}/cancel")
    public ResponseResult<AgentExecutionResultDTO> cancel(
            @PathVariable("actionId") Long actionId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRoleHeader,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String rolesHeader) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        String role = resolveRole(activeRoleHeader, rolesHeader);
        return ResponseResult.success(orchestrator.cancel(userId, role, actionId));
    }

    @GetMapping("/actions/{actionId}")
    public ResponseResult<AgentActionDTO> getAction(
            @PathVariable("actionId") Long actionId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRoleHeader,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String rolesHeader) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        String role = resolveRole(activeRoleHeader, rolesHeader);
        return ResponseResult.success(orchestrator.getAction(userId, role, actionId));
    }

    @GetMapping("/sessions")
    public ResponseResult<List<AgentSessionDTO>> listSessions(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRoleHeader,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String rolesHeader) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        String role = resolveRole(activeRoleHeader, rolesHeader);
        return ResponseResult.success(orchestrator.listSessions(userId, role));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseResult<AgentSessionDTO> getSession(
            @PathVariable("sessionId") Long sessionId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestHeader(value = CommonTraceConstants.ACTIVE_ROLE_HEADER, required = false) String activeRoleHeader,
            @RequestHeader(value = CommonTraceConstants.ROLES_HEADER, required = false) String rolesHeader) {
        Long userId = resolveUserId(userIdHeader);
        if (userId == null) {
            return ResponseResult.failure("缺少用户身份", 400);
        }
        String role = resolveRole(activeRoleHeader, rolesHeader);
        return ResponseResult.success(orchestrator.getSession(userId, role, sessionId));
    }

    private Long resolveUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        return Long.valueOf(userIdHeader);
    }

    private String resolveRole(String activeRoleHeader, String rolesHeader) {
        if (activeRoleHeader != null && !activeRoleHeader.isBlank()) {
            return activeRoleHeader;
        }
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            return rolesHeader.split(",")[0].trim();
        }
        return "USER";
    }
}
