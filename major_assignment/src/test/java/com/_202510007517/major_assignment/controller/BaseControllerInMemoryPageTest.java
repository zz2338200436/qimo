package com._202510007517.major_assignment.controller;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseControllerInMemoryPageTest {

    private final TestableBaseController controller = new TestableBaseController();

    @Test
    void buildSpringPageResponseFromInMemoryList_boundsOutOfRangePageAndSlicesLastPage() {
        List<Map<String, Object>> items = List.of(
                Map.of("id", 1L),
                Map.of("id", 2L),
                Map.of("id", 3L)
        );

        Map<String, Object> response = controller.buildSpringPageResponseFromInMemoryList(items, 9, 2);

        assertThat(response).containsEntry("totalElements", 3L);
        assertThat(response).containsEntry("totalPages", 2);
        assertThat(response).containsEntry("number", 1);
        assertThat(response).containsEntry("size", 2);
        assertThat(response).containsEntry("numberOfElements", 1);
        assertThat(response).containsEntry("first", false);
        assertThat(response).containsEntry("last", true);
        assertThat(response.get("content"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(Map.class))
                .singleElement()
                .extracting(item -> ((Map<?, ?>) item).get("id"))
                .isEqualTo(3L);
    }

    @Test
    void buildSpringPageResponseFromInMemoryList_returnsEmptyFirstPageForNoRows() {
        Map<String, Object> response = controller.buildSpringPageResponseFromInMemoryList(List.of(), 5, 10);

        assertThat(response).containsEntry("totalElements", 0L);
        assertThat(response).containsEntry("totalPages", 0);
        assertThat(response).containsEntry("number", 0);
        assertThat(response).containsEntry("size", 10);
        assertThat(response).containsEntry("numberOfElements", 0);
        assertThat(response).containsEntry("first", true);
        assertThat(response).containsEntry("last", true);
        assertThat(response.get("content")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test
    void buildPageResultFromInMemoryList_boundsOutOfRangePageAndSlicesLastPage() {
        List<Map<String, Object>> items = List.of(
                Map.of("id", 1L),
                Map.of("id", 2L),
                Map.of("id", 3L)
        );

        var response = controller.buildPageResultFromInMemoryList(items, 9, 2);

        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.getPageNumber()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isTrue();
        assertThat(response.getOffset()).isEqualTo(2);
        assertThat(response.getNumberOfElements()).isEqualTo(1);
        assertThat(response.isEmpty()).isFalse();
        assertThat(response.getContent())
                .singleElement()
                .extracting(item -> ((Map<?, ?>) item).get("id"))
                .isEqualTo(3L);
    }

    @Test
    void buildPageResultFromInMemoryList_returnsEmptyFirstPageForNoRows() {
        var response = controller.buildPageResultFromInMemoryList(List.of(), 5, 10);

        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.getPageNumber()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getNumberOfElements()).isEqualTo(0);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
        assertThat(response.getContent()).isEmpty();
    }

    private static final class TestableBaseController extends BaseController {
        @Override
        protected Map<String, Object> buildSpringPageResponseFromInMemoryList(List<?> content, int pageNum, int pageSize) {
            return super.buildSpringPageResponseFromInMemoryList(content, pageNum, pageSize);
        }

        @Override
        protected <T> com._202510007517.major_assignment.entity.dto.PageResult<T> buildPageResultFromInMemoryList(List<T> content, int pageNum, int pageSize) {
            return super.buildPageResultFromInMemoryList(content, pageNum, pageSize);
        }
    }
}
