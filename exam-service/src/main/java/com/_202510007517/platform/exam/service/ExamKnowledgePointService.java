package com._202510007517.platform.exam.service;

import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ExamKnowledgePointService {

    private final ExamRepository examRepository;

    public ExamKnowledgePointService(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public List<Map<String, Object>> listKnowledgePoints(Long teacherId, Long examId) {
        requireTeacherExam(teacherId, examId);
        return examRepository.findKnowledgePointIdsByExamId(examId)
                .stream()
                .map(ExamKnowledgePointService::toLegacyKnowledgePoint)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceKnowledgePoints(Long teacherId, Long examId, List<Long> knowledgePointIds) {
        requireTeacherExam(teacherId, examId);
        List<Long> distinctIds = knowledgePointIds == null
                ? List.of()
                : new LinkedHashSet<>(knowledgePointIds).stream().toList();
        examRepository.replaceExamKnowledgePoints(examId, distinctIds);
    }

    private ExamRecord requireTeacherExam(Long teacherId, Long examId) {
        ExamRecord exam = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (teacherId == null || (exam.getTeacherId() != null && !teacherId.equals(exam.getTeacherId()))) {
            throw new IllegalArgumentException("考试不存在");
        }
        return exam;
    }

    private static Map<String, Object> toLegacyKnowledgePoint(Long knowledgePointId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", knowledgePointId);
        item.put("knowledgePointId", knowledgePointId);
        item.put("pointName", "知识点 " + knowledgePointId);
        item.put("name", "知识点 " + knowledgePointId);
        return item;
    }
}
