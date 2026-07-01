package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.EarlyWarning;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.mapper.EarlyWarningMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EarlyWarningServiceImplTest {

    @Test
    void getWarningList_boundsOutOfRangePageBeforeQueryAndResponseMetadata() {
        EarlyWarningMapper mapper = mock(EarlyWarningMapper.class);

        EarlyWarningServiceImpl service = new EarlyWarningServiceImpl();
        ReflectionTestUtils.setField(service, "earlyWarningMapper", mapper);

        EarlyWarning warning = new EarlyWarning();
        warning.setId(3003L);
        warning.setWarningType("low_score");
        warning.setWarningMessage("成绩偏低");
        warning.setIsResolved(Boolean.FALSE);

        when(mapper.countWarningsByCondition(7L, null, null, null, null)).thenReturn(3L);
        when(mapper.findWarningsByCondition(7L, null, null, null, null, 2, 2)).thenReturn(List.of(warning));

        PageResult<EarlyWarning> result = service.getWarningList(7L, null, null, null, null, 9, 2);

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
        assertThat(result.getOffset()).isEqualTo(2L);
        assertThat(result.getNumberOfElements()).isEqualTo(1);
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.getContent()).extracting(EarlyWarning::getId).containsExactly(3003L);
        assertThat(result.getContent()).extracting(EarlyWarning::getStatus).containsExactly("pending");
        assertThat(result.getContent()).extracting(EarlyWarning::getReason).containsExactly("成绩偏低");
    }

    @Test
    void getWarningList_returnsFirstPageForEmptyResultSet() {
        EarlyWarningMapper mapper = mock(EarlyWarningMapper.class);

        EarlyWarningServiceImpl service = new EarlyWarningServiceImpl();
        ReflectionTestUtils.setField(service, "earlyWarningMapper", mapper);

        when(mapper.countWarningsByCondition(7L, null, null, null, null)).thenReturn(0L);
        when(mapper.findWarningsByCondition(7L, null, null, null, null, 0, 10)).thenReturn(List.of());

        PageResult<EarlyWarning> result = service.getWarningList(7L, null, null, null, null, 5, 10);

        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
        assertThat(result.getOffset()).isZero();
        assertThat(result.getNumberOfElements()).isZero();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getContent()).isEmpty();
    }
}
