package com._202510007517.platform.course.service;

import com._202510007517.platform.course.config.CourseCacheNames;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CourseApplicationServiceCacheContractTest {

    @Test
    void courseReadMethodsUseCourseCacheNamespaces() throws Exception {
        Method list = CourseApplicationService.class.getMethod(
                "listTeacherCourses", Long.class, String.class, String.class, String.class, String.class);
        Method detail = CourseApplicationService.class.getMethod("getCourse", Long.class);

        assertThat(list.getAnnotation(Cacheable.class).cacheNames()).containsExactly(CourseCacheNames.COURSE_LIST);
        assertThat(detail.getAnnotation(Cacheable.class).cacheNames()).containsExactly(CourseCacheNames.COURSE_DETAIL);
    }

    @Test
    void courseWriteMethodsEvictCourseCaches() throws Exception {
        Method create = CourseApplicationService.class.getMethod(
                "createCourse", Long.class, com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO.class);
        Method update = CourseApplicationService.class.getMethod(
                "updateCourse", Long.class, Long.class, com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO.class);
        Method delete = CourseApplicationService.class.getMethod("deleteCourse", Long.class);

        assertEvictsCourseCaches(create);
        assertEvictsCourseCaches(update);
        assertEvictsCourseCaches(delete);
    }

    private static void assertEvictsCourseCaches(Method method) {
        Caching caching = method.getAnnotation(Caching.class);
        assertThat(caching).isNotNull();
        assertThat(Arrays.stream(caching.evict())
                        .filter(CacheEvict::allEntries)
                        .flatMap(evict -> Arrays.stream(evict.cacheNames())))
                .contains(CourseCacheNames.COURSE_LIST, CourseCacheNames.COURSE_DETAIL);
    }
}
