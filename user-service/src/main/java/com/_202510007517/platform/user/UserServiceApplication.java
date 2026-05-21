package com._202510007517.platform.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com._202510007517.platform")
@ConfigurationPropertiesScan(basePackages = "com._202510007517.platform.user")
@EnableFeignClients(basePackages = "com._202510007517.platform")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
