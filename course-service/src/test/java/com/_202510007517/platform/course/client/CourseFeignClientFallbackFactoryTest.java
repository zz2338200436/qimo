package com._202510007517.platform.course.client;

import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.course.api.feign.CourseFeignClientFallbackFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseFeignClientFallbackFactoryTest {

    @Test
    void fallbackReturnsSafeDefaultsWhenCourseServiceIsUnavailable() {
        CourseFeignClient client = new CourseFeignClientFallbackFactory()
                .create(new RuntimeException("connection refused"));

        assertThat(client.getCourse(101L)).isNull();
        assertThat(client.listStudentClassIds(42L)).isEmpty();
        assertThat(client.listCourseAssignments(7L, 101L, null)).isEmpty();
        assertThat(client.listTeacherClasses(7L, null, null, null, null, null)).isEmpty();
    }
}
