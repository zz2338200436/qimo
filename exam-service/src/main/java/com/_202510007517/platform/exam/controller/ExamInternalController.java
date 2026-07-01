package com._202510007517.platform.exam.controller;

import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmitRequestDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.service.ExamApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/exams")
public class ExamInternalController {

    private final ExamApplicationService examApplicationService;

    public ExamInternalController(ExamApplicationService examApplicationService) {
        this.examApplicationService = examApplicationService;
    }

    @GetMapping
    public List<ExamDTO> listStudentExams(@RequestParam Long studentId) {
        Map<String, Object> page = examApplicationService.listStudentExams(
                studentId, 1, 100, "startTime", "DESC", null, null, true);
        Object content = page.get("content");
        if (!(content instanceof Iterable<?> exams)) {
            return List.of();
        }
        return toExamDtos(exams);
    }

    @GetMapping("/{examId}")
    public ExamDTO getStudentExam(
            @PathVariable Long examId,
            @RequestParam Long studentId) {
        return toExamDto(examApplicationService.getStudentExamDetail(studentId, examId));
    }

    @PostMapping("/{examId}/submissions")
    public ExamSubmissionDTO submitStudentExam(
            @PathVariable Long examId,
            @RequestBody ExamSubmitRequestDTO request) {
        return examApplicationService.submit(examId, request);
    }

    @GetMapping("/scores")
    public List<StudentScoreDTO> listStudentExamScores(@RequestParam Long studentId) {
        return examApplicationService.listStudentExamScores(studentId);
    }

    @GetMapping("/{examId}/teacher")
    public ExamDTO getTeacherExam(
            @PathVariable Long examId,
            @RequestParam Long teacherId) {
        return examApplicationService.getTeacherExam(teacherId, examId);
    }

    @PostMapping("/teacher")
    public ExamDTO createTeacherExam(
            @RequestParam Long teacherId,
            @RequestBody TeacherExamUpsertRequestDTO request) {
        return toExamDto(examApplicationService.createTeacherExam(teacherId, request));
    }

    @PutMapping("/{examId}/teacher")
    public ExamDTO updateTeacherExam(
            @PathVariable Long examId,
            @RequestParam Long teacherId,
            @RequestBody TeacherExamUpsertRequestDTO request) {
        return toExamDto(examApplicationService.updateTeacherExam(teacherId, examId, request));
    }

    @DeleteMapping("/{examId}/teacher")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacherExam(
            @PathVariable Long examId,
            @RequestParam Long teacherId) {
        examApplicationService.deleteTeacherExam(teacherId, examId);
    }

    @GetMapping("/{examId}/knowledge-point-ids")
    public List<Long> listKnowledgePointIds(@PathVariable Long examId) {
        return examApplicationService.listKnowledgePointIds(examId);
    }

    private static ExamDTO toExamDto(ExamRecord record) {
        ExamDTO dto = new ExamDTO();
        dto.setId(record.getId());
        dto.setTitle(record.getTitle());
        dto.setDescription(record.getDescription());
        dto.setCourseId(record.getCourseId());
        dto.setStartTime(record.getStartTime());
        dto.setEndTime(record.getEndTime());
        dto.setPublishDate(record.getPublishDate());
        dto.setIsActive(record.getActive());
        dto.setIsOnline(record.getOnline());
        dto.setDuration(record.getDuration());
        dto.setTotalScore(record.getTotalScore());
        return dto;
    }

    private static List<ExamDTO> toExamDtos(Iterable<?> exams) {
        java.util.ArrayList<ExamDTO> result = new java.util.ArrayList<>();
        for (Object exam : exams) {
            if (exam instanceof Map<?, ?> map) {
                result.add(toExamDto(map));
            }
        }
        return result;
    }

    private static ExamDTO toExamDto(Map<?, ?> map) {
        ExamDTO dto = new ExamDTO();
        dto.setId(asLong(map.get("id")));
        dto.setTitle(asString(map.get("title")));
        dto.setDescription(asString(map.get("description")));
        dto.setCourseId(asLong(map.get("courseId")));
        dto.setCourseName(asString(map.get("courseName")));
        dto.setStartTime(asString(map.get("startTime")));
        dto.setEndTime(asString(map.get("endTime")));
        dto.setPublishDate(asString(map.get("publishDate")));
        dto.setIsActive(asBoolean(map.get("isActive")));
        dto.setIsOnline(asBoolean(map.get("isOnline")));
        dto.setDuration(asInteger(map.get("duration")));
        dto.setTotalScore(asInteger(firstPresent(map, "totalScore", "maxScore")));
        dto.setStatus(asString(map.get("status")));
        return dto;
    }

    private static Object firstPresent(Map<?, ?> map, String firstKey, String secondKey) {
        Object value = map.get(firstKey);
        return value == null ? map.get(secondKey) : value;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(String.valueOf(value));
    }
}
