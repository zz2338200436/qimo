package com._202510007517.platform.exam.config;

import com._202510007517.platform.common.exception.BaseGlobalExceptionHandler;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExamServiceExceptionHandler extends BaseGlobalExceptionHandler {

    public ExamServiceExceptionHandler(Environment environment) {
        super(environment);
    }
}
