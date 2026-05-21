package com._202510007517.platform.common.autoconfigure;

import com._202510007517.platform.common.feign.FeignRequestInterceptor;
import com._202510007517.platform.common.feign.GlobalFeignErrorDecoder;
import com._202510007517.platform.common.management.CommonReactiveManagementAccessFilter;
import com._202510007517.platform.common.management.CommonServletManagementAccessFilter;
import com._202510007517.platform.common.management.ManagementEndpointAccessMatcher;
import com._202510007517.platform.common.management.ManagementEndpointAccessProperties;
import com._202510007517.platform.common.mdc.CommonReactiveMdcFilter;
import com._202510007517.platform.common.mdc.CommonServletMdcFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

@AutoConfiguration
@EnableConfigurationProperties(ManagementEndpointAccessProperties.class)
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ManagementEndpointAccessMatcher managementEndpointAccessMatcher(
            ManagementEndpointAccessProperties properties) {
        return new ManagementEndpointAccessMatcher(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RequestInterceptor.class)
    public RequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ErrorDecoder.class)
    public ErrorDecoder globalFeignErrorDecoder(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new GlobalFeignErrorDecoder(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<CommonServletManagementAccessFilter> commonServletManagementAccessFilter(
            ManagementEndpointAccessMatcher matcher) {
        FilterRegistrationBean<CommonServletManagementAccessFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CommonServletManagementAccessFilter(matcher));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<CommonServletMdcFilter> commonServletMdcFilter() {
        FilterRegistrationBean<CommonServletMdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CommonServletMdcFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter commonReactiveManagementAccessFilter(ManagementEndpointAccessMatcher matcher) {
        return new CommonReactiveManagementAccessFilter(matcher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public WebFilter commonReactiveMdcFilter() {
        return new CommonReactiveMdcFilter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "commonMeterRegistryCustomizer")
    public MeterRegistryCustomizer<MeterRegistry> commonMeterRegistryCustomizer(
            @Value("${spring.application.name:unknown-service}") String applicationName) {
        return registry -> registry.config().commonTags("service", applicationName);
    }
}
