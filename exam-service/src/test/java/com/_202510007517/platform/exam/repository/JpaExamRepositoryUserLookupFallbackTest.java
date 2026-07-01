package com._202510007517.platform.exam.repository;

import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaExamRepositoryUserLookupFallbackTest {

    @Mock
    private ExamJpaRepository examJpaRepository;

    @Mock
    private ExamSubmissionJpaRepository submissionJpaRepository;

    @Mock
    private ExamClassJpaRepository examClassJpaRepository;

    @Mock
    private ExamKnowledgePointJpaRepository knowledgePointJpaRepository;

    @Mock
    private ExamQuestionJpaRepository questionJpaRepository;

    @Mock
    private UserLookupJpaRepository userLookupJpaRepository;

    @Test
    void upsertSubmissionDoesNotFailWhenStudentNameLookupTableIsMissing() {
        JpaExamRepository repository = new JpaExamRepository(
                examJpaRepository,
                submissionJpaRepository,
                examClassJpaRepository,
                knowledgePointJpaRepository,
                questionJpaRepository,
                userLookupJpaRepository);
        ExamEntity exam = new ExamEntity();
        exam.setId(9001L);
        exam.setTitle("Midterm");
        ExamSubmissionEntity savedSubmission = new ExamSubmissionEntity();
        savedSubmission.setId(9101L);
        savedSubmission.setExamId(9001L);
        savedSubmission.setStudentId(42L);
        savedSubmission.setContent("{\"q1\":\"A\"}");
        savedSubmission.setSubmissionDate(LocalDateTime.of(2026, 6, 11, 10, 20));
        savedSubmission.setTimeTaken(35);
        savedSubmission.setGraded(false);

        when(examJpaRepository.findById(9001L)).thenReturn(Optional.of(exam));
        when(submissionJpaRepository.findByExamIdAndStudentId(9001L, 42L))
                .thenReturn(Optional.empty(), Optional.of(savedSubmission));
        when(submissionJpaRepository.save(any(ExamSubmissionEntity.class))).thenReturn(savedSubmission);
        ExamSubmissionRecord record = repository.upsertSubmission(9001L, 42L, 35, "{\"q1\":\"A\"}");

        assertThat(record.getId()).isEqualTo(9101L);
        assertThat(record.getStudentId()).isEqualTo(42L);
        assertThat(record.getStudentName()).isNull();
        verifyNoInteractions(userLookupJpaRepository);
    }

    @Test
    void findSubmissionDoesNotQueryStudentNameLookupTable() {
        JpaExamRepository repository = new JpaExamRepository(
                examJpaRepository,
                submissionJpaRepository,
                examClassJpaRepository,
                knowledgePointJpaRepository,
                questionJpaRepository,
                userLookupJpaRepository);
        ExamEntity exam = new ExamEntity();
        exam.setId(9001L);
        exam.setTitle("Midterm");
        ExamSubmissionEntity submission = new ExamSubmissionEntity();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("{\"q1\":\"A\"}");
        submission.setSubmissionDate(LocalDateTime.of(2026, 6, 11, 10, 20));
        submission.setTimeTaken(35);
        submission.setGraded(false);

        when(examJpaRepository.findById(9001L)).thenReturn(Optional.of(exam));
        when(submissionJpaRepository.findByExamIdAndStudentId(9001L, 42L))
                .thenReturn(Optional.of(submission));

        ExamSubmissionRecord record = repository.findSubmission(9001L, 42L).orElseThrow();

        assertThat(record.getId()).isEqualTo(9101L);
        assertThat(record.getStudentId()).isEqualTo(42L);
        assertThat(record.getStudentName()).isNull();
        verifyNoInteractions(userLookupJpaRepository);
    }

    @Test
    void findSubmissionsByExamIdDoesNotQueryStudentNameLookupTable() {
        JpaExamRepository repository = new JpaExamRepository(
                examJpaRepository,
                submissionJpaRepository,
                examClassJpaRepository,
                knowledgePointJpaRepository,
                questionJpaRepository,
                userLookupJpaRepository);
        ExamEntity exam = new ExamEntity();
        exam.setId(9001L);
        exam.setTitle("Midterm");
        ExamSubmissionEntity submission = submission();

        when(examJpaRepository.findById(9001L)).thenReturn(Optional.of(exam));
        when(submissionJpaRepository.findByExamIdOrderBySubmissionDateDescIdDesc(9001L))
                .thenReturn(List.of(submission));

        List<ExamSubmissionRecord> records = repository.findSubmissionsByExamId(9001L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStudentName()).isNull();
        verifyNoInteractions(userLookupJpaRepository);
    }

    @Test
    void findSubmissionsByTeacherIdDoesNotQueryStudentNameLookupTable() {
        JpaExamRepository repository = new JpaExamRepository(
                examJpaRepository,
                submissionJpaRepository,
                examClassJpaRepository,
                knowledgePointJpaRepository,
                questionJpaRepository,
                userLookupJpaRepository);
        ExamEntity exam = new ExamEntity();
        exam.setId(9001L);
        exam.setTitle("Midterm");
        exam.setTeacherId(7L);
        ExamSubmissionEntity submission = submission();

        when(examJpaRepository.findByTeacherIdOrderByStartTimeDescIdDesc(7L)).thenReturn(List.of(exam));
        when(submissionJpaRepository.findByExamIdIn(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(submission));

        List<ExamSubmissionRecord> records = repository.findSubmissionsByTeacherId(
                7L, 9001L, 42L, false, "id", "DESC", 0, 10);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStudentName()).isNull();
        verifyNoInteractions(userLookupJpaRepository);
    }

    @Test
    void findSubmissionByIdDoesNotQueryStudentNameLookupTable() {
        JpaExamRepository repository = new JpaExamRepository(
                examJpaRepository,
                submissionJpaRepository,
                examClassJpaRepository,
                knowledgePointJpaRepository,
                questionJpaRepository,
                userLookupJpaRepository);
        ExamEntity exam = new ExamEntity();
        exam.setId(9001L);
        exam.setTitle("Midterm");
        ExamSubmissionEntity submission = submission();

        when(examJpaRepository.findById(9001L)).thenReturn(Optional.of(exam));
        when(submissionJpaRepository.findById(9101L)).thenReturn(Optional.of(submission));

        ExamSubmissionRecord record = repository.findSubmissionById(9101L).orElseThrow();

        assertThat(record.getId()).isEqualTo(9101L);
        assertThat(record.getStudentName()).isNull();
        verifyNoInteractions(userLookupJpaRepository);
    }

    private ExamSubmissionEntity submission() {
        ExamSubmissionEntity submission = new ExamSubmissionEntity();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("{\"q1\":\"A\"}");
        submission.setSubmissionDate(LocalDateTime.of(2026, 6, 11, 10, 20));
        submission.setTimeTaken(35);
        submission.setGraded(false);
        return submission;
    }
}
