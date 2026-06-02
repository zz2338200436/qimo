package com._202510007517.major_assignment.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseControllerStatusNormalizationTest {

    private static final class TestController extends BaseController {
        String normalizeAssignment(String rawStatus) {
            return normalizeAssignmentStatusFilter(rawStatus);
        }

        Boolean resolveAssignmentActive(String rawStatus, Boolean fallback) {
            return resolveAssignmentActiveFilter(rawStatus, fallback);
        }

        String normalizeExam(String rawStatus) {
            return normalizeExamStatusFilter(rawStatus);
        }
    }

    private final TestController controller = new TestController();

    @Test
    void normalizesAssignmentStatusAliases() {
        assertThat(controller.normalizeAssignment("pending")).isEqualTo("pending");
        assertThat(controller.normalizeAssignment("待提交")).isEqualTo("pending");
        assertThat(controller.normalizeAssignment(" 已评分 ")).isEqualTo("graded");
        assertThat(controller.normalizeAssignment("已结束")).isEqualTo("closed");
    }

    @Test
    void resolvesAssignmentActiveFilterFromNormalizedStatus() {
        assertThat(controller.resolveAssignmentActive("已提交", null)).isTrue();
        assertThat(controller.resolveAssignmentActive("closed", true)).isFalse();
        assertThat(controller.resolveAssignmentActive("unknown", false)).isFalse();
        assertThat(controller.resolveAssignmentActive(null, true)).isTrue();
    }

    @Test
    void normalizesExamStatusAliases() {
        assertThat(controller.normalizeExam("upcoming")).isEqualTo("upcoming");
        assertThat(controller.normalizeExam("即将开始")).isEqualTo("upcoming");
        assertThat(controller.normalizeExam("进行中")).isEqualTo("ongoing");
        assertThat(controller.normalizeExam("已完成")).isEqualTo("completed");
        assertThat(controller.normalizeExam("已评分")).isEqualTo("graded");
    }
}
