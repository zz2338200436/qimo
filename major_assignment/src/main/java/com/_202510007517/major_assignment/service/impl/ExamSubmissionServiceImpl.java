package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.mapper.ExamSubmissionMapper;
import com._202510007517.major_assignment.service.ExamSubmissionService;
import com._202510007517.major_assignment.service.KnowledgeMasteryService;
import com._202510007517.major_assignment.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ExamSubmissionServiceImpl implements ExamSubmissionService {
    
    @Autowired
    private ExamSubmissionMapper submissionMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private KnowledgeMasteryService knowledgeMasteryService;
    
    @Override
    public ExamSubmission submitExam(Long examId, Long studentId, Integer timeTaken, Map<String, String> answers) {
        ExamSubmission existingSubmission = submissionMapper.findByExamAndStudent(examId, studentId);
        ExamSubmission submission = existingSubmission != null ? existingSubmission : new ExamSubmission();
        submission.setExamId(examId);
        submission.setStudentId(studentId);
        submission.setSubmissionDate(new Date());
        submission.setGraded(false);
        submission.setScore(0);
        submission.setTimeTaken(timeTaken);
        submission.setTeacherComment(null);
        
        // 将answers转换为JSON字符串存储到content字段
        if (answers != null && !answers.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String contentJson = objectMapper.writeValueAsString(answers);
                submission.setContent(contentJson);
            } catch (Exception e) {
                // 如果JSON转换失败，直接存储toString
                submission.setContent(answers.toString());
            }
        }
        
        int result;
        if (existingSubmission != null) {
            result = submissionMapper.fullUpdateSubmission(submission);
        } else {
            result = submissionMapper.insertSubmission(submission);
        }
        
        if (result > 0) {
            return submission;
        }
        return null;
    }
    
    @Override
    public ExamSubmission getSubmissionByExamAndStudent(Long examId, Long studentId) {
        return submissionMapper.findByExamAndStudent(examId, studentId);
    }
    
    @Override
    public ExamSubmission gradeExam(Long submissionId, Integer score, String teacherComment) {
        // 根据submissionId找到对应的提交记录
        ExamSubmission submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            return null;
        }
        
        // 更新提交记录的分数、教师评语和批改状态
        submission.setScore(score);
        submission.setTeacherComment(teacherComment);
        submission.setGraded(true);
        
        // 保存更新
        int result = submissionMapper.updateGrade(submission);
        if (result > 0) {
            // 如果批改成功，自动更新知识点掌握情况
            try {
                Long studentId = submission.getStudentId();
                Long examId = submission.getExamId();
                
                // 获取考试信息以获取总分（假设考试总分为100，实际应该从exam表获取）
                Integer totalScore = 100;
                
                // 更新知识点掌握情况
                knowledgeMasteryService.updateMasteryByExam(submissionId, studentId, examId, totalScore);
            } catch (Exception e) {
                // 静默处理异常，不影响批改结果
            }
            
            return submission;
        }
        return null;
    }
    
    @Override
    public List<ExamSubmission> getSubmissionsByExamId(Long examId) {
        return submissionMapper.findByExamId(examId);
    }
    
    @Override
    public List<ExamSubmission> getAllSubmissions() {
        return submissionMapper.findAll();
    }
    
    @Override
    public ExamSubmission getSubmissionById(Long submissionId) {
        return submissionMapper.findById(submissionId);
    }
    
    @Override
    public List<ExamSubmission> getSubmissionsByStudentId(Long studentId) {
        return submissionMapper.findByStudentId(studentId);
    }
    
    @Override
    public boolean deleteSubmission(Long submissionId) {
        return submissionMapper.deleteSubmission(submissionId) > 0;
    }
    
    @Override
    public boolean updateSubmission(ExamSubmission submission) {
        return submissionMapper.fullUpdateSubmission(submission) > 0;
    }
    
    @Override
    public List<ExamSubmission> getSubmissionsWithPagination(Integer page, Integer size, Integer total, String sortBy, String order, Long examId, Long studentId, Boolean graded) {
        PageUtils.PageWindow window = PageUtils.resolvePageWindow(
                page == null ? 1 : page,
                size == null ? PageUtils.DEFAULT_PAGE_SIZE : size,
                total == null ? 0 : total);
        return submissionMapper.findWithPagination(examId, studentId, graded, sortBy, order, window.offset(), window.size());
    }
    
    @Override
    public Integer countSubmissions(Long examId, Long studentId, Boolean graded) {
        return submissionMapper.countSubmissions(examId, studentId, graded);
    }
}
