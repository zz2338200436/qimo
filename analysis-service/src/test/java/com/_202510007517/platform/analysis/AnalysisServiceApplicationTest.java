package com._202510007517.platform.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServiceApplicationTest {

    @Test
    void scansPlatformPackageForFeignFallbackFactories() {
        SpringBootApplication annotation = AnalysisServiceApplication.class.getAnnotation(SpringBootApplication.class);

        assertThat(annotation.scanBasePackages()).contains("com._202510007517.platform");
    }
}
