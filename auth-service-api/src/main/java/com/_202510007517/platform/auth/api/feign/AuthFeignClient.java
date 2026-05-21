package com._202510007517.platform.auth.api.feign;

import com._202510007517.platform.auth.api.dto.AuthContextDTO;
import com._202510007517.platform.auth.api.dto.TokenIntrospectionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", path = "/internal/auth")
public interface AuthFeignClient {

    @GetMapping("/context/{userId}")
    AuthContextDTO getAuthContext(@PathVariable("userId") Long userId);

    @PostMapping("/introspect")
    TokenIntrospectionDTO introspect(@RequestHeader("Authorization") String authorization);
}
