package com._202510007517.platform.exam.controller;

import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.service.ExamApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/exams")
public class ExamInternalController {

    private final ExamApplicationService examApplicationService;

    public ExamInternalController(ExamApplicationService examApplicationService) {
        this.examApplicationService = examApplicationService;
    }

    @GetMapping("/{examId}/teacher")
    public ExamDTO getTeacherExam(
            @PathVariable Long examId,
            @RequestParam Long teacherId) {
        return examApplicationService.getTeacherExam(teacherId, examId);
    }

    @GetMapping("/{examId}/knowledge-point-ids")
    public List<Long> listKnowledgePointIds(@PathVariable Long examId) {
        return examApplicationService.listKnowledgePointIds(examId);
    }
}
