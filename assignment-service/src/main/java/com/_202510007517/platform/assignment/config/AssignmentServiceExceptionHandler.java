package com._202510007517.platform.assignment.config;

import com._202510007517.platform.common.exception.BaseGlobalExceptionHandler;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AssignmentServiceExceptionHandler extends BaseGlobalExceptionHandler {

    public AssignmentServiceExceptionHandler(Environment environment) {
        super(environment);
    }
}
