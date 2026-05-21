package com._202510007517.platform.ai.config;

import com._202510007517.platform.common.exception.BaseGlobalExceptionHandler;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiServiceExceptionHandler extends BaseGlobalExceptionHandler {

    public AiServiceExceptionHandler(Environment environment) {
        super(environment);
    }
}
