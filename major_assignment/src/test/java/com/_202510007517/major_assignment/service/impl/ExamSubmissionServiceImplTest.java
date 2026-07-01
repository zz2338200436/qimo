package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.mapper.ExamSubmissionMapper;
import com._202510007517.major_assignment.service.KnowledgeMasteryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamSubmissionServiceImplTest {

    @Test
    void submitExamUpdatesExistingSubmissionInsteadOfInsertingDuplicate() {
        ExamSubmissionMapper submissionMapper = mock(ExamSubmissionMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        KnowledgeMasteryService knowledgeMasteryService = mock(KnowledgeMasteryService.class);

        ExamSubmissionServiceImpl service = new ExamSubmissionServiceImpl();
        ReflectionTestUtils.setField(service, "submissionMapper", submissionMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "knowledgeMasteryService", knowledgeMasteryService);

        ExamSubmission existing = new ExamSubmission();
        existing.setId(55L);
        existing.setExamId(9001L);
        existing.setStudentId(42L);
        existing.setGraded(false);
        existing.setScore(0);

        when(submissionMapper.findByExamAndStudent(9001L, 42L)).thenReturn(existing);
        when(submissionMapper.fullUpdateSubmission(any(ExamSubmission.class))).thenReturn(1);

        ExamSubmission result = service.submitExam(9001L, 42L, 12, Map.of("content", "updated answer"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(55L);
        assertThat(result.getExamId()).isEqualTo(9001L);
        assertThat(result.getStudentId()).isEqualTo(42L);
        assertThat(result.getTimeTaken()).isEqualTo(12);
        assertThat(result.getContent()).contains("updated answer");
        verify(submissionMapper, never()).insertSubmission(any(ExamSubmission.class));
        verify(submissionMapper).fullUpdateSubmission(any(ExamSubmission.class));
    }
}
