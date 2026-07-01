package com._202510007517.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayCorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(GatewayCorsProperties properties) {
        return new CorsWebFilter(exchange -> {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(properties.getAllowedOrigins());
            configuration.setAllowedMethods(properties.getAllowedMethods());
            configuration.setAllowedHeaders(properties.getAllowedHeaders());
            configuration.setExposedHeaders(properties.getExposedHeaders());
            configuration.setAllowCredentials(properties.isAllowCredentials());
            configuration.setMaxAge(properties.getMaxAgeSeconds());

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source.getCorsConfiguration(exchange);
        });
    }
}
