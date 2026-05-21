package com._202510007517.major_assignment.controller;

import org.junit.jupiter.api.Test;

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

    private static final class TestController extends BaseController {
        int clamp(int pageSize) {
            return clampPageSize(pageSize);
        }
    }
}
