package com._202510007517.platform.gateway.controller;

import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/_fallback/{service}")
    public Mono<ResponseEntity<ResponseResult<Map<String, Object>>>> fallback(@PathVariable String service) {
        ResponseResult<Map<String, Object>> body = ResponseResult.<Map<String, Object>>
                        failure(service + " 暂时不可用", 503)
                .data(Map.of("service", service));
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }

    @RequestMapping("/_fallback/registry/{service}")
    public Mono<ResponseEntity<ResponseResult<Map<String, Object>>>> registryFallback(@PathVariable String service) {
        ResponseResult<Map<String, Object>> body = ResponseResult.<Map<String, Object>>
                        failure(service + " 暂无可用实例", 504)
                .data(Map.of("service", service));
        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body));
    }
}
