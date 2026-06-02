package com._202510007517.major_assignment.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageUtilsTest {

    @Test
    void resolvePageWindow_clampsSizeAndBoundsOutOfRangePage() {
        PageUtils.PageWindow window = PageUtils.resolvePageWindow(9, 200, 3);

        assertThat(window.page()).isEqualTo(1);
        assertThat(window.size()).isEqualTo(100);
        assertThat(window.totalElements()).isEqualTo(3);
        assertThat(window.totalPages()).isEqualTo(1);
        assertThat(window.offset()).isEqualTo(0);
    }

    @Test
    void resolvePageWindow_returnsFirstPageForEmptyResults() {
        PageUtils.PageWindow window = PageUtils.resolvePageWindow(-5, 0, 0);

        assertThat(window.page()).isEqualTo(1);
        assertThat(window.size()).isEqualTo(PageUtils.DEFAULT_PAGE_SIZE);
        assertThat(window.totalElements()).isEqualTo(0);
        assertThat(window.totalPages()).isEqualTo(0);
        assertThat(window.offset()).isEqualTo(0);
    }
}
