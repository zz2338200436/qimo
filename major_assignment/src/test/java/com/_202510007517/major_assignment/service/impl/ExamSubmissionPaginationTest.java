package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.mapper.ExamSubmissionMapper;
import com._202510007517.major_assignment.service.KnowledgeMasteryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamSubmissionPaginationTest {

    @Test
    void getSubmissionsWithPagination_usesProvidedTotalToAvoidDuplicateCountQuery() {
        ExamSubmissionMapper submissionMapper = mock(ExamSubmissionMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        KnowledgeMasteryService knowledgeMasteryService = mock(KnowledgeMasteryService.class);

        ExamSubmissionServiceImpl service = new ExamSubmissionServiceImpl();
        ReflectionTestUtils.setField(service, "submissionMapper", submissionMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "knowledgeMasteryService", knowledgeMasteryService);

        ExamSubmission submission = new ExamSubmission();
        submission.setId(8801L);

        when(submissionMapper.findWithPagination(11L, 42L, false, "id", "DESC", 0, 100))
                .thenReturn(List.of(submission));

        List<ExamSubmission> result = service.getSubmissionsWithPagination(9, 1000, 3, "id", "DESC", 11L, 42L, false);

        assertThat(result).extracting(ExamSubmission::getId).containsExactly(8801L);
        verify(submissionMapper).findWithPagination(11L, 42L, false, "id", "DESC", 0, 100);
    }
}
