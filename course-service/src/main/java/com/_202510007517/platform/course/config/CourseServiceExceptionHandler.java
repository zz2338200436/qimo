package com._202510007517.platform.course.config;

import com._202510007517.platform.common.exception.BaseGlobalExceptionHandler;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CourseServiceExceptionHandler extends BaseGlobalExceptionHandler {

    public CourseServiceExceptionHandler(Environment environment) {
        super(environment);
    }
}
