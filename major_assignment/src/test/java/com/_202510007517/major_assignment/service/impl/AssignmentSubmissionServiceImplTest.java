package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.AssignmentSubmission;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.mapper.AssignmentSubmissionMapper;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import com._202510007517.major_assignment.service.KnowledgeMasteryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentSubmissionServiceImplTest {

    @Test
    void getSubmissionsWithPagination_usesProvidedTotalToAvoidDuplicateCountQuery() {
        AssignmentSubmissionMapper submissionMapper = mock(AssignmentSubmissionMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        KnowledgeMasteryService knowledgeMasteryService = mock(KnowledgeMasteryService.class);
        EarlyWarningAnalysisService earlyWarningAnalysisService = mock(EarlyWarningAnalysisService.class);

        AssignmentSubmissionServiceImpl service = new AssignmentSubmissionServiceImpl();
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", submissionMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "knowledgeMasteryService", knowledgeMasteryService);
        ReflectionTestUtils.setField(service, "earlyWarningAnalysisService", earlyWarningAnalysisService);

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(7001L);

        when(submissionMapper.findWithPagination(9L, 42L, true, "id", "DESC", 0, 100))
                .thenReturn(List.of(submission));

        List<AssignmentSubmission> result = service.getSubmissionsWithPagination(9, 1000, 3, "id", "DESC", 9L, 42L, true);

        assertThat(result).extracting(AssignmentSubmission::getId).containsExactly(7001L);
        verify(submissionMapper).findWithPagination(9L, 42L, true, "id", "DESC", 0, 100);
    }
}
