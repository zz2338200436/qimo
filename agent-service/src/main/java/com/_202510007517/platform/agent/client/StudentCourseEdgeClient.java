package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.api.dto.CourseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "studentCourseEdgeClient", name = "course-service", path = "/api/student/courses")
public interface StudentCourseEdgeClient {

    @GetMapping("/{courseId}")
    ResponseResult<CourseDTO> getStudentCourse(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("courseId") Long courseId);
}
