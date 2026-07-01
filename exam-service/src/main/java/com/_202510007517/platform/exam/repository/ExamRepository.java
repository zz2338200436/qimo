package com._202510007517.platform.exam.repository;

import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.domain.ExamQuestionRecord;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import com._202510007517.platform.exam.api.dto.StudentScoreDTO;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExamRepository {
    List<ExamRecord> findByClassIds(List<Long> classIds);

    List<ExamRecord> findByTeacherId(Long teacherId);

    boolean isExamVisibleToClasses(Long examId, List<Long> classIds);

    Optional<ExamRecord> findExam(Long examId);

    Optional<ExamSubmissionRecord> findSubmission(Long examId, Long studentId);

    ExamSubmissionRecord upsertSubmission(Long examId, Long studentId, Integer timeTaken, String contentJson);

    ExamRecord insert(ExamRecord exam);

    void update(ExamRecord exam);

    void delete(Long examId);

    void replaceExamClasses(Long examId, Set<Long> classIds);

    List<Long> findKnowledgePointIdsByExamId(Long examId);

    void replaceExamKnowledgePoints(Long examId, List<Long> knowledgePointIds);

    List<ExamQuestionRecord> findQuestionsByExamId(Long examId);

    void replaceExamQuestions(Long examId, List<ExamQuestionRecord> questions);

    int countSubmissionsByExamId(Long examId);

    List<ExamSubmissionRecord> findSubmissionsByExamId(Long examId);

    List<ExamSubmissionRecord> findSubmissionsByTeacherId(Long teacherId,
                                                          Long examId,
                                                          Long studentId,
                                                          Boolean graded,
                                                          String sortBy,
                                                          String order,
                                                          int offset,
                                                          int limit);

    int countSubmissionsByTeacherId(Long teacherId, Long examId, Long studentId, Boolean graded);

    Optional<ExamSubmissionRecord> findSubmissionById(Long submissionId);

    int updateSubmission(Long submissionId, ExamSubmissionRecord submission);

    int updateSubmissionGrade(Long submissionId, Integer score, String teacherComment);

    int deleteSubmission(Long submissionId);

    List<StudentScoreDTO> findStudentExamScores(Long studentId);
}
