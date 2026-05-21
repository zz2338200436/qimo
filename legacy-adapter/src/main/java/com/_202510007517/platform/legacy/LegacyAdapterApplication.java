package com._202510007517.platform.legacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com._202510007517.platform")
public class LegacyAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyAdapterApplication.class, args);
    }
}
