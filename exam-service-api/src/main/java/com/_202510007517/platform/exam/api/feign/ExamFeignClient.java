package com._202510007517.platform.exam.api.feign;

import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmitRequestDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "exam-service", path = "/internal/exams")
public interface ExamFeignClient {

    @GetMapping
    List<ExamDTO> listByStudent(@RequestParam("studentId") Long studentId);

    @GetMapping("/{examId}")
    ExamDTO getExam(@PathVariable("examId") Long examId,
                    @RequestParam("studentId") Long studentId);

    @GetMapping("/{examId}/teacher")
    ExamDTO getTeacherExam(@PathVariable("examId") Long examId,
                           @RequestParam("teacherId") Long teacherId);

    @GetMapping("/{examId}/knowledge-point-ids")
    List<Long> listKnowledgePointIds(@PathVariable("examId") Long examId);

    @PostMapping("/{examId}/submissions")
    ExamSubmissionDTO submit(@PathVariable("examId") Long examId,
                             @RequestBody ExamSubmitRequestDTO request);

    @GetMapping("/scores")
    List<StudentScoreDTO> listScores(@RequestParam("studentId") Long studentId);
}
