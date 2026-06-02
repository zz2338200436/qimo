package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.Course;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseControllerCourseResolutionTest {

    private static final class TestableBaseController extends BaseController {
        Long resolve(String rawValue, List<Course> courses) {
            return resolveTeacherCourseId(rawValue, courses);
        }

        Long resolveFromPayload(Map<String, Object> payload, List<Course> courses) {
            return resolveTeacherCourseIdFromPayload(payload, courses);
        }
    }

    @Test
    void returnsNumericCourseIdDirectly() {
        TestableBaseController controller = new TestableBaseController();

        assertThat(controller.resolve("42", List.of())).isEqualTo(42L);
    }

    @Test
    void resolvesCourseIdFromCourseCode() {
        TestableBaseController controller = new TestableBaseController();
        Course course = new Course();
        course.setId(7L);
        course.setCourseCode("CS101");

        assertThat(controller.resolve("CS101", List.of(course))).isEqualTo(7L);
    }

    @Test
    void returnsNullWhenCourseCodeCannotBeResolved() {
        TestableBaseController controller = new TestableBaseController();
        Course course = new Course();
        course.setId(7L);
        course.setCourseCode("CS101");

        assertThat(controller.resolve("MISSING", List.of(course))).isNull();
    }

    @Test
    void returnsNullWhenRawValueMissing() {
        TestableBaseController controller = new TestableBaseController();

        assertThat(controller.resolve(null, List.of())).isNull();
        assertThat(controller.resolve("", List.of())).isNull();
    }

    @Test
    void resolvesCourseIdFromPayloadUsingNumberField() {
        TestableBaseController controller = new TestableBaseController();
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", 12);

        assertThat(controller.resolveFromPayload(payload, List.of())).isEqualTo(12L);
    }

    @Test
    void resolvesCourseIdFromPayloadUsingCourseCodeField() {
        TestableBaseController controller = new TestableBaseController();
        Course course = new Course();
        course.setId(9L);
        course.setCourseCode("JAVA202");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "JAVA202");

        assertThat(controller.resolveFromPayload(payload, List.of(course))).isEqualTo(9L);
    }

    @Test
    void resolvesCourseIdFromPayloadUsingLegacyCourseIdKey() {
        TestableBaseController controller = new TestableBaseController();
        Map<String, Object> payload = new HashMap<>();
        payload.put("course_id", 5L);

        assertThat(controller.resolveFromPayload(payload, List.of())).isEqualTo(5L);
    }

    @Test
    void returnsNullWhenPayloadMissingCourseField() {
        TestableBaseController controller = new TestableBaseController();

        assertThat(controller.resolveFromPayload(Map.of(), List.of())).isNull();
        assertThat(controller.resolveFromPayload(null, List.of())).isNull();
    }
}
