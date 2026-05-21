package com._202510007517.platform.user.config;

import com._202510007517.platform.common.exception.BaseGlobalExceptionHandler;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserServiceExceptionHandler extends BaseGlobalExceptionHandler {

    public UserServiceExceptionHandler(Environment environment) {
        super(environment);
    }
}
