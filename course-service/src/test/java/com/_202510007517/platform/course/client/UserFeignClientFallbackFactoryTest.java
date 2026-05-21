package com._202510007517.platform.course.client;

import com._202510007517.platform.common.exception.RemoteServerException;
import com._202510007517.platform.user.api.feign.UserFeignClientFallbackFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserFeignClientFallbackFactoryTest {

    @Test
    void fallbackTranslatesUserServiceOutageTo503() {
        UserFeignClientFallbackFactory factory = new UserFeignClientFallbackFactory();
        RuntimeException cause = new RuntimeException("connection refused");

        assertThatThrownBy(() -> factory.create(cause).getProfile(7L))
                .isInstanceOf(RemoteServerException.class)
                .satisfies(ex -> {
                    RemoteServerException remote = (RemoteServerException) ex;
                    assertThat(remote.getCode()).isEqualTo(503);
                    assertThat(remote.getMessage()).contains("user-service");
                    assertThat(remote.getBodyJson()).contains("connection refused");
                });
    }
}
