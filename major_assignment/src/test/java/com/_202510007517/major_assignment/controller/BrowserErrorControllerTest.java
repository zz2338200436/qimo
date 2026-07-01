package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.BrowserError;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.BrowserErrorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowserErrorControllerTest {

    @Test
    void getBrowserErrorList_returnsSharedPageResultFromService() {
        BrowserErrorService browserErrorService = mock(BrowserErrorService.class);

        BrowserErrorController controller = new BrowserErrorController();
        ReflectionTestUtils.setField(controller, "browserErrorService", browserErrorService);

        BrowserError error = new BrowserError();
        error.setId(9001L);
        error.setErrorMessage("TypeError: x is undefined");

        PageResult<BrowserError> pageResult = new PageResult<>();
        pageResult.setContent(java.util.List.of(error));
        pageResult.setPageNumber(2);
        pageResult.setPageSize(2);
        pageResult.setTotalElements(3L);
        pageResult.setTotalPages(2);
        pageResult.setFirst(false);
        pageResult.setLast(true);
        pageResult.setOffset(2L);
        pageResult.setNumberOfElements(1);
        pageResult.setEmpty(false);

        when(browserErrorService.getBrowserErrorList(Map.of("level", "error"), 9, 2)).thenReturn(pageResult);

        ResponseResult<PageResult<BrowserError>> response = controller.getBrowserErrorList(Map.of("level", "error"), 9, 2);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getPageNumber()).isEqualTo(2);
        assertThat(response.getData().getPageSize()).isEqualTo(2);
        assertThat(response.getData().getTotalElements()).isEqualTo(3L);
        assertThat(response.getData().getTotalPages()).isEqualTo(2);
        assertThat(response.getData().getContent()).extracting(BrowserError::getId).containsExactly(9001L);
    }
}
