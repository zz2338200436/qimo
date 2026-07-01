package com._202510007517.platform.auth.config;

import com._202510007517.platform.common.exception.BaseGlobalExceptionHandler;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthServiceExceptionHandler extends BaseGlobalExceptionHandler {

    public AuthServiceExceptionHandler(Environment environment) {
        super(environment);
    }
}
