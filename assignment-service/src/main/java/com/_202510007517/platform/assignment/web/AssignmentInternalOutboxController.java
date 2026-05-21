package com._202510007517.platform.assignment.web;

import com._202510007517.platform.assignment.service.AssignmentOutboxAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/assignments/outbox")
public class AssignmentInternalOutboxController {

    private final AssignmentOutboxAdminService assignmentOutboxAdminService;

    public AssignmentInternalOutboxController(AssignmentOutboxAdminService assignmentOutboxAdminService) {
        this.assignmentOutboxAdminService = assignmentOutboxAdminService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return assignmentOutboxAdminService.status();
    }

    @PostMapping("/relay")
    public Map<String, Object> relayOnce() {
        return Map.of("publishedCount", assignmentOutboxAdminService.relayOnce());
    }
}
