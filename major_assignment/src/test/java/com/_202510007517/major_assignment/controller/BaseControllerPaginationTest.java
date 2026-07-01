package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.utils.PageUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseControllerPaginationTest {

    private final TestController controller = new TestController();

    @Test
    void clampPageSize_maps_any_generated_integer_to_safe_range() {
        Random random = new Random(10L);

        for (int i = 0; i < 10_000; i++) {
            int input = random.nextInt();
            int clamped = controller.clamp(input);

            assertTrue(clamped >= 1 && clamped <= 100,
                    () -> "expected clamped page size in [1, 100], input=" + input + ", clamped=" + clamped);
        }
    }

    @Test
    void clampPageSize_keeps_values_inside_range_unchanged() {
        for (int pageSize = 1; pageSize <= 100; pageSize++) {
            assertEquals(pageSize, controller.clamp(pageSize));
        }
    }

    @Test
    void clampPageSize_covers_required_boundary_samples() {
        assertEquals(10, controller.clamp(Integer.MIN_VALUE));
        assertEquals(10, controller.clamp(0));
        assertEquals(50, controller.clamp(50));
        assertEquals(100, controller.clamp(Integer.MAX_VALUE));
    }

    @Test
    void calculateTotalPages_returnsZeroForEmptyResults() {
        assertEquals(0, controller.totalPages(0, 10));
    }

    @Test
    void calculateTotalPages_usesClampedPageSize() {
        assertEquals(1, controller.totalPages(5, 0));
        assertEquals(2, controller.totalPages(101, 100));
    }

    @Test
    void boundPageNum_clampsToFirstPageWhenInputInvalid() {
        assertEquals(1, controller.boundPage(0, 35, 10));
        assertEquals(1, controller.boundPage(-8, 35, 10));
    }

    @Test
    void boundPageNum_clampsToLastPageWhenInputTooLarge() {
        assertEquals(4, controller.boundPage(99, 35, 10));
    }

    @Test
    void boundPageNum_returnsFirstPageForEmptyResults() {
        assertEquals(1, controller.boundPage(9, 0, 10));
    }

    @Test
    void buildSpringPageResponse_matchesExpectedShape() {
        Map<String, Object> page = controller.springPage(List.of("a", "b"), 2, 10, 25);

        assertEquals(3, page.get("totalPages"));
        assertEquals(25L, page.get("totalElements"));
        assertEquals(10, page.get("size"));
        assertEquals(1, page.get("number"));
        assertEquals(false, page.get("first"));
        assertEquals(false, page.get("last"));
        assertEquals(2, page.get("numberOfElements"));
        assertEquals(false, page.get("empty"));

        @SuppressWarnings("unchecked")
        Map<String, Object> pageable = (Map<String, Object>) page.get("pageable");
        assertEquals(1, pageable.get("pageNumber"));
        assertEquals(10, pageable.get("pageSize"));
        assertEquals(10, pageable.get("offset"));
    }

    @Test
    void buildSpringPageResponse_clampsInputsAndHandlesEmptyResults() {
        Map<String, Object> page = controller.springPage(List.of(), 99, 0, 0);

        assertEquals(0, page.get("totalPages"));
        assertEquals(10, page.get("size"));
        assertEquals(0, page.get("number"));
        assertEquals(true, page.get("first"));
        assertEquals(true, page.get("last"));
        assertEquals(true, page.get("empty"));

        @SuppressWarnings("unchecked")
        Map<String, Object> pageable = (Map<String, Object>) page.get("pageable");
        assertEquals(0, pageable.get("pageNumber"));
        assertEquals(10, pageable.get("pageSize"));
        assertEquals(0, pageable.get("offset"));
    }

    @Test
    void resolvePageWindow_reusesSharedPaginationRules() {
        PageUtils.PageWindow window = controller.window(99, 0, 35);

        assertEquals(4, window.page());
        assertEquals(10, window.size());
        assertEquals(35L, window.totalElements());
        assertEquals(4, window.totalPages());
        assertEquals(30, window.offset());
    }

    private static final class TestController extends BaseController {
        int clamp(int pageSize) {
            return clampPageSize(pageSize);
        }

        int totalPages(int totalElements, int pageSize) {
            return calculateTotalPages(totalElements, pageSize);
        }

        int boundPage(int pageNum, int totalElements, int pageSize) {
            return boundPageNum(pageNum, totalElements, pageSize);
        }

        Map<String, Object> springPage(List<?> content, int pageNum, int pageSize, int totalElements) {
            return buildSpringPageResponse(content, pageNum, pageSize, totalElements);
        }

        PageUtils.PageWindow window(int pageNum, int pageSize, int totalElements) {
            return resolvePageWindow(pageNum, pageSize, totalElements);
        }
    }
}
